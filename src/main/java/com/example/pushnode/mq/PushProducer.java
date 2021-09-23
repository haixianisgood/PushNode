package com.example.pushnode.mq;

import com.example.pushnode.aip.msg.MessageModel;
import com.example.pushnode.db.redis.RedisRepository;
import com.google.gson.Gson;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

//mq的生产者类，向mq发送消息，用于在不同节点交换消息
@Component
public class PushProducer {
    @Autowired
    @Qualifier("messageProducer")
    private DefaultMQProducer producer;

    @Autowired
    private RedisRepository repository;

    @Value("${appconfig.rocketmq.topic}")
    private String topic;

    private final Gson gson = new Gson();

    /**
     * 
     * @param pushNodeTag
     * @param message
     */
    public void sendMessage(String pushNodeTag, MessageModel message, SendCallback callback) {
        Message mqMessage = new Message(topic, pushNodeTag,
                gson.toJson(message).getBytes(StandardCharsets.UTF_8));
        try {
            producer.send(mqMessage, callback);
        } catch (MQClientException | RemotingException | InterruptedException e) {
            e.printStackTrace();
            repository.storeMessage(message);
        }
    }
}
