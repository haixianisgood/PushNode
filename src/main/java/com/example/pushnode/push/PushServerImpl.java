package com.example.pushnode.push;

import com.example.pushnode.aip.msg.MessageModel;
import com.example.pushnode.db.redis.RedisRepository;
import com.example.pushnode.route.RouteService;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Map;

@Component
public class PushServerImpl implements PushServer {
    @Autowired
    @Qualifier("bossGroup")
    public EventLoopGroup bossGroup;

    @Autowired
    @Qualifier("workerGroup")
    public EventLoopGroup workerGroup;

    @Autowired
    private Map<String, ChannelPipeline> deviceMap;

    @Autowired
    private RouteService routeService;

    @Autowired
    private ChannelInitializer<NioSocketChannel> initializer;

    @Autowired
    private RedisRepository repository;

    @Value("${appconfig.push.port}")
    private int port;

    @Value("${appconfig.push.name}")
    private String nodeName;

    private final ServerBootstrap serverBootstrap = new ServerBootstrap();
    private ChannelFuture channelFuture;

    @Override
    public void pushToDevice(String deviceId, MessageModel message) {
        ChannelPipeline pipeline = deviceMap.get(deviceId);
        if(pipeline != null) {
            System.out.println("push to device "+message.getDstDeviceId());
            pipeline.writeAndFlush(message).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    //如果发送失败，就存入redis，并且与设备断开连接
                    if (future.isDone() && !future.isSuccess()) {
                        repository.storeMessage(message);
                        pipeline.close();
                    }
                }
            });
        }
    }

    @PostConstruct
    @Override
    public void start() {
        serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childHandler(initializer);
        channelFuture = serverBootstrap.bind(port);
        routeService.addPushNode(nodeName);
    }

    @PreDestroy
    @Override
    public void close() {
        routeService.deletePushNode(nodeName);
        channelFuture.channel().close();
    }

    @Override
    public void closeDevice(String deviceId) {
        deviceMap.remove(deviceId);
    }
}
