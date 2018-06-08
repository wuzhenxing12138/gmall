package com.atguigu.gmall.order.mqListener;

import com.atguigu.gmall.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.MapMessage;

@Component
public class OrderPayListener {//SKU_DEDUCT_QUEUE

    @Autowired
    OrderService orderService;

    @JmsListener(destination = "SKU_DEDUCT_QUEUE",containerFactory = "jmsQueueListener")
    public void consumerGwareDeductQueue(MapMessage mm) throws Exception{

        // 得到消息内容
        String orderId = mm.getString("orderId");
        String status = mm.getString("status");

        // 订单状态修改为已经出库(已减库存)
        orderService.updateOrderStatusById(orderId,status);

    }


    @JmsListener(destination = "PAYMENT_RESULT_QUEUE",containerFactory = "jmsQueueListener")
    public void consumerPaymentResult(MapMessage mm) throws Exception{

        // 得到消息内容
        String out_trade_no = mm.getString("out_trade_no");
        String trade_status = mm.getString("trade_status");

        // 更新订单状态
        orderService.updateOrderStatus(out_trade_no,trade_status);

        System.out.println("通过消息队列，更新订单状态。。。");

        // 发布订单成功的消息
        orderService.sendOrderResult(out_trade_no,trade_status);// 状态修改为通知库存

    }

}
