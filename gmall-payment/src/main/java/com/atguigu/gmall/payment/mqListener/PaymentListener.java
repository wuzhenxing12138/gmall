package com.atguigu.gmall.payment.mqListener;

import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.MapMessage;
import java.util.Date;

@Component
public class PaymentListener {

    @Autowired
    PaymentService paymentService;

    @JmsListener(destination = "PAYMENT_CHECK_QUEUE", containerFactory = "jmsQueueListener")
    public void consumerPaymentResult(MapMessage mm) throws Exception {

        // 得到消息内容
        String out_trade_no = mm.getString("out_trade_no");
        int i = mm.getInt("counter");

        // 调用支付宝查询接口，检查支付情况
        String trrade_status = paymentService.checkPaymentStatus(out_trade_no);
        System.out.println("检查支付情况的队列消费，调用支付宝查询接口，检查支付情况" + new Date() + ",次数=" + i);

        // 通知下一次检查的时间和次数
        if(trrade_status!=null){
            if (!trrade_status.equals("TRADE_SUCCESS")&&i>0) {
                // 继续检查
                paymentService.sendDelayPaymentResult(out_trade_no, (i - 1));
            }else{
                if(trrade_status.equals("TRADE_SUCCESS")){
                    paymentService.sendPaymentResult(out_trade_no,"");
                }
            }
        }else{
            paymentService.sendDelayPaymentResult(out_trade_no, (i - 1));
        }

    }
}
