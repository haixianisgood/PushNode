package com.example.pushnode.test;

import com.example.pushnode.ApplicationConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@RunWith(SpringRunner.class)
public class MqTest {
    public String tag = "${Tag:}";

    @Autowired
    private ApplicationConfig config;

    @Test
    public void t1() {
        System.out.println(config.getTag());
    }
}
