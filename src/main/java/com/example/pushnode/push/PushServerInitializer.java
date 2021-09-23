package com.example.pushnode.push;

import com.example.pushnode.SpringContextHolder;
import com.example.pushnode.netty.handler.MessageWrapperCodec;
import com.example.pushnode.netty.handler.MessageHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 初始化推送服务器的socket的channel
 * pipeline模型
 *
 *     in --> 长度域解码 --> UTF-8字符串解码 --> JSON解码 --> 消息处理 --> 空闲状态处理
 *     out <-- 长度域编码 <-- UTF-8字符串编码 <-- JSON编码 <-- PushMessage
 */
@Component("initializer")
public class PushServerInitializer extends ChannelInitializer<NioSocketChannel> {

    @Override
    protected void initChannel(NioSocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        MessageHandler messageHandler = SpringContextHolder.getBean(MessageHandler.class);

        pipeline.addLast(new LengthFieldBasedFrameDecoder(4*1024, 0, 4, 0, 4));
        pipeline.addLast(new LengthFieldPrepender(4));
        pipeline.addLast(new StringEncoder(StandardCharsets.UTF_8));
        pipeline.addLast(new StringDecoder(StandardCharsets.UTF_8));
        pipeline.addLast(new MessageWrapperCodec());
        pipeline.addLast(messageHandler);
        pipeline.addLast(new IdleStateHandler(35, 35, 35));
    }
}
