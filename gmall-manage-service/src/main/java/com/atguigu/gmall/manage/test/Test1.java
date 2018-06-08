package com.atguigu.gmall.manage.test;


import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.util.RedisUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import redis.clients.jedis.Jedis;

import java.io.IOException;

@RunWith(SpringRunner.class)
@SpringBootTest
public class Test1 {

    @Autowired
    RedisUtil  redisUtil;

    @Test
    public void a() {

        Jedis jedis = redisUtil.getJedis();

        String s = jedis.get("sku:73:info");

        System.out.println(s);
    }
}
