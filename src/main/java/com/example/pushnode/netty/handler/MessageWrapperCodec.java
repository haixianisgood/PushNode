package com.example.pushnode.netty.handler;

import com.example.pushnode.aip.msg.MessageModel;
import com.example.pushnode.aip.msg.MessageType;
import com.example.pushnode.aip.msg.MessageWrapper;
import com.google.gson.Gson;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;

import java.util.List;

@ChannelHandler.Sharable
public class MessageWrapperCodec extends MessageToMessageCodec<String, Object> {
    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, List<Object> out) throws Exception {
        MessageWrapper messageWrapper = new MessageWrapper();
        if(msg instanceof MessageModel) {
            messageWrapper.setType(MessageType.TYPE_MESSAGE);
            messageWrapper.setMessageModel((MessageModel) msg);

            Gson gson = new Gson();
            out.add(gson.toJson(messageWrapper));
        }
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, String msg, List<Object> out) throws Exception {
        //System.out.println("msg: "+msg);
        Gson gson = new Gson();
        MessageWrapper messageWrapper = gson.fromJson(msg, MessageWrapper.class);

        //System.out.println("from client "+messageWrapper);
        if(!messageWrapper.isHeartbeat()) {
            out.add(messageWrapper.getPayload());
        }
    }
}
