package com.example.pushnode.mq;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class MqConfig {
    @Value("${appconfig.rocketmq.group}")
    private String group;

    @Value("${appconfig.rocketmq.topic}")
    private String topic;

    @Value("${appconfig.rocketmq.tag}")
    private String tag;

    @Value("${appconfig.rocketmq.name-server}")
    private String nameServer;

    /**
     * mq消费者的bean。
     * 用于接收要发给连接了当前推送节点的设备的消息
     * @param listener 消息监听器
     * @return 消费者bean
     */
    @Bean(initMethod = "start", destroyMethod = "shutdown")
    public DefaultMQPushConsumer messageConsumer(@Autowired MessageListenerConcurrently listener) {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(group);
        consumer.setNamesrvAddr(nameServer);
        try {
            consumer.subscribe(topic, tag);
        } catch (MQClientException e) {
            e.printStackTrace();
        }

        consumer.setMessageListener(listener);

        return consumer;
    }

    /**
     * mq生产者bean。用于向mq发送消息
     * @return mq生产者bean
     */
    @Bean(initMethod = "start", destroyMethod = "shutdown", name = "messageProducer")
    public DefaultMQProducer messageProducer() {
        DefaultMQProducer producer = new DefaultMQProducer(group);
        producer.setNamesrvAddr(nameServer);
        producer.setRetryTimesWhenSendAsyncFailed(3);
        producer.setRetryTimesWhenSendFailed(3);

        return producer;
    }
}
