package com.example.pushnode.mq;

import com.example.pushnode.aip.msg.MessageModel;
import com.example.pushnode.push.PushServer;
import com.google.gson.Gson;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 对mq消息监听接口的实现类
 */
@Component
public class PushMessageListener implements MessageListenerConcurrently {
    @Autowired
    private PushServer pushServer;

    private Gson gson = new Gson();

    @Override
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
        for(MessageExt msg : msgs) {
            MessageModel messageModel = gson.fromJson(new String(msg.getBody(),
                    StandardCharsets.UTF_8), MessageModel.class);
            pushServer.pushToDevice(messageModel.getDstDeviceId(), messageModel);
        }

        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }
}
