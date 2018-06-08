package com.atguigu.gmall.payment.service.impl;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.service.PaymentService;
import com.atguigu.gmall.util.ActiveMQUtil;
import org.apache.activemq.ScheduledMessage;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.apache.activemq.command.ActiveMQMessage;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.Date;
import java.util.List;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    PaymentInfoMapper paymentInfoMapper;

    @Autowired
    ActiveMQUtil activeMQUtil;

    @Autowired
    AlipayClient alipayClient;

    @Override
    public void savePaymentInfo(PaymentInfo paymentInfo){

        PaymentInfo paymentInfoQuery=new PaymentInfo();
        paymentInfoQuery.setOrderId(paymentInfo.getOrderId());
        List<PaymentInfo> list = paymentInfoMapper.select(paymentInfoQuery);
        if(list.size()>0){
            return ;
        }
        paymentInfoMapper.insertSelective(paymentInfo);

    }

    @Override
    public PaymentInfo getPaymentInfo(PaymentInfo paymentInfoQuery){
        PaymentInfo paymentInfo= paymentInfoMapper.selectOne(paymentInfoQuery);
        return  paymentInfo;
    }

    @Override
    public void updatePaymentInfo(String outTradeNo,PaymentInfo paymentInfo){

        Example example=new Example(PaymentInfo.class);
        example.createCriteria().andEqualTo("outTradeNo",outTradeNo);

        paymentInfoMapper.updateByExampleSelective(paymentInfo,example);

        //update payment_info set a = ? ,set b = ? ... where out_trade_no = 123
        return   ;
    }

    @Override
    public void sendPaymentResult(String out_trade_no, String trade_success) {


        Connection connection = activeMQUtil.getConnection();
        try {
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);//开启消息事务，分布式事务

            // 队列式的消息，不管多少个consumer，只要有一个消费，任务完成，不在重复消费消息
            Queue queue = session.createQueue("PAYMENT_RESULT_QUEUE");

            MessageProducer producer = session.createProducer(queue);

            MapMessage mapMessage = new ActiveMQMapMessage();
            mapMessage.setString("trade_status",trade_success);
            mapMessage.setString("out_trade_no",out_trade_no);

            producer.setDeliveryMode(DeliveryMode.PERSISTENT);//设置消息的是否持久化
            producer.send(mapMessage);//发送消息

            session.commit();//开启事务了，必须提交

            producer.close();//关闭会话
            session.close();// 关闭会话
            connection.close();//关闭连接
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    @Override
    public void sendDelayPaymentResult(String outTradeNo, int i) {
        Connection connection = activeMQUtil.getConnection();
        try {
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);//开启消息事务，分布式事务

            // 队列式的消息，不管多少个consumer，只要有一个消费，任务完成，不在重复消费消息
            Queue queue = session.createQueue("PAYMENT_CHECK_QUEUE");

            MessageProducer producer = session.createProducer(queue);

            MapMessage mapMessage = new ActiveMQMapMessage();
            mapMessage.setString("out_trade_no",outTradeNo);
            mapMessage.setInt("counter",i);
            mapMessage.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY,1000*60);//该队列会按照i时间后，被消费

            producer.setDeliveryMode(DeliveryMode.PERSISTENT);//设置消息的是否持久化
            producer.send(mapMessage);//发送消息

            session.commit();//开启事务了，必须提交

            producer.close();//关闭会话
            session.close();// 关闭会话
            connection.close();//关闭连接
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public String checkPaymentStatus(String out_trade_no) {
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        request.setBizContent("{" +
                "    \"out_trade_no\":\""+out_trade_no+"\"" +
                "  }");
        AlipayTradeQueryResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }


        if(response.isSuccess()){
            String tradeStatus = response.getTradeStatus();
            // 用out_trade_no检查一次当前的out_trade_no对应的支付状态，如果为不是已支付状态，才继续修改状态，发送消息
            boolean ifPaid = getIfPaid(out_trade_no, tradeStatus);

            if(!ifPaid){
                if(tradeStatus.equals("TRADE_SUCCESS")){
                    System.out.println("调用成功");
                    //
                    // 更新支付信息表
                    String trade_no = response.getTradeNo();
                    PaymentInfo paymentInfo = new PaymentInfo();
                    paymentInfo.setAlipayTradeNo(System.currentTimeMillis()+"");
                    paymentInfo.setCallbackTime(new Date());
                    paymentInfo.setCallbackContent("支付宝的返回的参数的json字符串");
                    paymentInfo.setPaymentStatus("已支付");
                    updatePaymentInfo(out_trade_no,paymentInfo);

                    // 1 触发支付成功消息
                    // 发送订单支付成功的消息通知，异步启动订单系统处理已支付订单
                    sendPaymentResult(out_trade_no,response.getTradeStatus());//payment_result_queue
             }
            }
        } else {
            System.out.println("调用失败");
        }

        return response.getTradeStatus();
    }

    @Override
    public boolean getIfPaid(String out_trade_no, String trade_success) {

        boolean b = false;

        PaymentInfo p = new PaymentInfo();
        p.setOutTradeNo(out_trade_no);

        // 查询刚才的已经支付的订单
        p.setPaymentStatus("已支付");

        List<PaymentInfo> select = paymentInfoMapper.select(p);

        if(select!=null&&select.size()>0){
            b = true;
        }

        return b;
    }


}
