package com.example.pushnode.db.redis;

import com.example.pushnode.aip.msg.MessageModel;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class RedisRepository {
    @Autowired
    private RedissonClient redissonClient;

    /**
     * 存储消息，TTL为24h，超过24h由其他业务实现存储
     * @param message 消息
     */
    public void storeMessage(MessageModel message) {
        String dstDeviceId = message.getDstDeviceId();

        RBucket<MessageModel> bucket = redissonClient.getBucket("Message:"+message.getId());
        RSet<String> messageSet = redissonClient.getSet(
                "Messages:"+dstDeviceId);
        messageSet.add(message.getId());
        messageSet.expire(1, TimeUnit.DAYS);
        bucket.set(message, 1, TimeUnit.DAYS);
    }

    /**
     * 删除消息。在设备确收到消息后，就从redis删除消息
     * @param deviceId 设备id
     * @param messageId 消息id
     */
    public void deleteMessage(String deviceId, String messageId) {
        RBucket<MessageModel> bucket = redissonClient.getBucket("Message:"+messageId);
        RSet<String> messageSet = redissonClient.getSet("Messages:"+deviceId);
        bucket.delete();
        messageSet.remove(messageId);
    }

    /**
     * 获取未读消息
     * @param deviceId 设备id
     * @return 未读消息的列表
     */
    public List<MessageModel> getUnreadMessages(String deviceId) {
        List<MessageModel> unreadMessages = new ArrayList<>();

        RSet<String> messageSet = redissonClient.getSet("Messages:"+deviceId);
        for(String messageId : messageSet) {
            RBucket<MessageModel> bucket = redissonClient.getBucket("Message:"+messageId);
            if(bucket.isExists()) {
                unreadMessages.add(bucket.get());
            }
        }


        return unreadMessages;
    }
}
