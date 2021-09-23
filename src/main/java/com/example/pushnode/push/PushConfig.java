package com.example.pushnode.push;

import io.netty.channel.ChannelPipeline;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class PushConfig {
    /**
     * 推送节点的设备路由表，Map<设备id， socket>
     * @return 可以并发执行的map
     */
    @Bean("deviceMap")
    public Map<String, ChannelPipeline> deviceMap() {
        return new ConcurrentHashMap<>(50000);
    }
}
