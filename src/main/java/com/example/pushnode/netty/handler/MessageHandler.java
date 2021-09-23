package com.example.pushnode.netty.handler;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.exceptions.*;
import com.example.pushnode.aip.msg.AckModel;
import com.example.pushnode.aip.msg.AuthModel;
import com.example.pushnode.aip.msg.MessageModel;
import com.example.pushnode.db.redis.RedisRepository;
import com.example.pushnode.mq.PushProducer;
import com.example.pushnode.route.RouteService;
import com.example.pushnode.route.lock.DistributedLock;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.timeout.IdleStateEvent;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * netty的pipeline中，用于处理与客户端设备业务的主要逻辑
 * 其中，对设备的登录、发送消息，需要加锁。保证在持有锁期间，不会有其他线程对目标设备进行操作
 */
@Component
@Scope("prototype")
public class MessageHandler extends ChannelInboundHandlerAdapter {
    @Autowired
    private PushProducer pushProducer;

    @Autowired
    private RouteService routeService;

    @Autowired
    private Map<String, ChannelPipeline> deviceMap;

    @Autowired
    private RedisRepository repository;

    @Value("${appconfig.push.name}")
    private String nodeName;

    private String currentDeviceId;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if(msg instanceof MessageModel) {
            pushMessage(ctx, (MessageModel) msg);
        }

        if(msg instanceof AuthModel) {
            doAuth(ctx, (AuthModel) msg);
        }

        if(msg instanceof AckModel) {
            acknowledge((AckModel) msg);
        }
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        //从本节点的“设备-pipeline”表中删除
        deviceMap.remove(currentDeviceId);
        //从路由表中删除
        routeService.deleteDevice(nodeName, currentDeviceId);

        ctx.fireChannelInactive();

        System.out.println(currentDeviceId+" logout");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();

        ctx.close();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if(evt instanceof IdleStateEvent) {
            ctx.close();
        }
    }

    /**
     * 客户端发来的是需要推送到其他设备的消息
     * @param message 消息模型
     */
    private void pushMessage(ChannelHandlerContext ctx, MessageModel message) {
        String dstDeviceId = message.getDstDeviceId();

        DistributedLock lock = routeService.distributedLock(dstDeviceId);
        lock.lock();

        message.setSrcDeviceId(currentDeviceId);


        //通过rocket mq作为中间件，发送到相应设备的推送节点
        //在线则直接推送到设备
        if(routeService.isOnline(dstDeviceId)) {
            String pushNodeTag = routeService.getPushNode(dstDeviceId);
            System.out.println("push to node: "+pushNodeTag);
            if(pushNodeTag != null) {
                pushProducer.sendMessage(pushNodeTag, message, new SendCallback() {
                    @Override
                    public void onSuccess(SendResult sendResult) {

                    }

                    @Override
                    public void onException(Throwable e) {
                        e.printStackTrace();
                        repository.storeMessage(message);
                        ctx.close();
                    }
                });
            }
        } else {
            //若不在线，存储到redis
            repository.storeMessage(message);
        }

        lock.unlock();
    }

    /**
     * 对客户端进行鉴权
     * @param ctx 当前channel的ctx
     * @param auth 鉴权信息模型
     */
    private void doAuth(ChannelHandlerContext ctx, AuthModel auth) {
        String deviceId = auth.getDeviceId();

        DistributedLock lock = routeService.distributedLock(deviceId);
        lock.lock();

        System.out.println(deviceId+" login");

        //如果设备已经在线，就强制下线
        if(routeService.isOnline(deviceId)) {
            String nodeName = routeService.getPushNode(deviceId);
            routeService.deleteDevice(nodeName, deviceId);
        }

        currentDeviceId = deviceId;

        //通过token判别该连接是否合法
        /*if(!checkToken(auth)) {
            ctx.close();
            return;
        }*/

        //构建路由信息
        deviceMap.put(currentDeviceId, ctx.pipeline());
        routeService.addDevice(nodeName, deviceId);

        //获取未读消息
        List<MessageModel> unreadMessages = repository.getUnreadMessages(currentDeviceId);

        lock.unlock();

        for(MessageModel message : unreadMessages) {
            ctx.write(message);
        }

        ctx.flush();
    }

    /**
     * 客户端收到消息后发来确认
     * @param ack 消息确认模型
     */
    private void acknowledge(AckModel ack) {
        repository.deleteMessage(currentDeviceId, ack.getAckId());
    }

    private boolean checkToken(AuthModel authModel) {
        DecodedJWT decodedJWT;

        try {
            decodedJWT = JWT.decode(authModel.getToken());
        } catch (JWTDecodeException e) {
            e.printStackTrace();
            return false;
        }

        String deviceId = decodedJWT.getSubject();

        if(!deviceId.equals(authModel.getDeviceId())) {
            return false;
        }

        JWTVerifier jwtVerifier = JWT.require(Algorithm.HMAC256("push")).build();
        try {
            jwtVerifier.verify(authModel.getToken());
        } catch (JWTVerificationException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }
}
