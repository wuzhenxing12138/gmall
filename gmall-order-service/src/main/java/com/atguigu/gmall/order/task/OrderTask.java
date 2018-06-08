package com.atguigu.gmall.order.task;

import com.atguigu.gmall.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class OrderTask {

    @Autowired
    OrderService orderService;

    @Scheduled(cron = "0 0/2 * * * ?")
    public void work() throws InterruptedException {
        System.out.println("thread = ===============" + Thread.currentThread());
    }
}
