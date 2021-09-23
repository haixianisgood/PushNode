package com.example.pushnode.zk;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ZooKeeperConfig {
    @Value("${appconfig.zookeeper.connect-string}")
    private String connectString;

    @Bean(initMethod = "start", destroyMethod = "close")
    public CuratorFramework curatorFramework() {
        return CuratorFrameworkFactory.builder()
                .sessionTimeoutMs(10*1000)
                .namespace("Push")
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .connectString(connectString)
                .build();
    }
}
