package com.example.pushnode.db.redis;

import com.alibaba.fastjson.support.config.FastJsonConfig;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.FstCodec;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {
    @Autowired
    @Qualifier("workerGroup")
    private EventLoopGroup workerGroup;

    @Value("${appconfig.redis.address}")
    private String redisAddress;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.setCodec(new FastJsonCodec())
                .setEventLoopGroup(workerGroup)
                .useSingleServer()
                .setAddress(redisAddress)
                .setKeepAlive(true)
                .setTcpNoDelay(true);

        return Redisson.create(config);
    }
}
