package com.atguigu.gmall.order.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.OrderDetail;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.order.mapper.OrderDetailMapper;
import com.atguigu.gmall.order.mapper.OrderInfoMapper;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.util.ActiveMQUtil;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.List;
import java.util.UUID;


@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    OrderInfoMapper orderInfoMapper;

    @Autowired
    OrderDetailMapper orderDetailMapper;

    @Autowired
    ActiveMQUtil activeMQUtil;

    @Override
    public boolean checkTradeCode(String userId, String tradeCode) {

        boolean b = false;

        Jedis jedis = redisUtil.getJedis();
        String tradeCodeRedis = jedis.get("user:" + userId + ":tradeCode");

        if (StringUtils.isNotBlank(tradeCodeRedis) && tradeCode.equals(tradeCodeRedis)) {
            b = true;
            jedis.del("user:" + userId + ":tradeCode");
        }
        jedis.close();
        return b;
    }

    @Override
    public String genTradeCode(String userId) {
        Jedis jedis = redisUtil.getJedis();

        // 生成tradeCode，然后将它存到redis中一份儿
        String tradeCode = UUID.randomUUID().toString();
        jedis.setex("user:" + userId + ":tradeCode", 60 * 10, tradeCode);

        jedis.close();

        return tradeCode;
    }

    @Override
    public OrderInfo saveOrder(OrderInfo orderInfo) {

        // 保存订单，返回订单主键
        orderInfoMapper.insertSelective(orderInfo);

        // 循环保存订单详情，设置订单主键
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            orderDetail.setOrderId(orderInfo.getId());
            orderDetailMapper.insert(orderDetail);
        }
        return orderInfo;
    }


    @Override
    public OrderInfo getOrderById(String orderId) {

        OrderInfo orderInfo = orderInfoMapper.selectByPrimaryKey(orderId);

        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setOrderId(orderId);
        List<OrderDetail> select = orderDetailMapper.select(orderDetail);

        orderInfo.setOrderDetailList(select);
        return orderInfo;
    }

    @Override
    public String seletTestOutTradeNo() {
        String no = orderInfoMapper.seletTestOutTradeNo();
        return no;
    }

    @Override
    public void updateOrderStatus(String out_trade_no, String trade_status) {

        Example e = new Example(OrderInfo.class);

        e.createCriteria().andEqualTo("outTradeNo", out_trade_no);

        OrderInfo o = new OrderInfo();

        if (trade_status.equals("TRADE_SUCCESS")) {
            o.setOrderStatus("支付成功");
            orderInfoMapper.updateByExampleSelective(o, e);
        }

    }

    @Override
    public OrderInfo getOrderByOutTradeNo(String out_trade_no) {
        OrderInfo o = new OrderInfo();
        o.setOutTradeNo(out_trade_no);
        OrderInfo orderInfo = orderInfoMapper.selectOne(o);

        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setOrderId(orderInfo.getId());
        List<OrderDetail> select = orderDetailMapper.select(orderDetail);

        orderInfo.setOrderDetailList(select);
        return orderInfo;
    }

    @Override
    public void sendOrderResult(String out_trade_no, String trade_status) {

        Connection connection = activeMQUtil.getConnection();
        try {
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);//开启消息事务，分布式事务

            // 队列式的消息，不管多少个consumer，只要有一个消费，任务完成，不在重复消费消息
            Queue queue = session.createQueue("ORDER_RESULT_QUEUE");

            MessageProducer producer = session.createProducer(queue);

            // 封装订单详情
            OrderInfo o = getOrderByOutTradeNo(out_trade_no);
            TextMessage textMessage = new ActiveMQTextMessage();
            textMessage.setText(JSON.toJSONString(o));

            producer.setDeliveryMode(DeliveryMode.PERSISTENT);//设置消息的是否持久化
            producer.send(textMessage);//发送消息

            session.commit();//开启事务了，必须提交

            producer.close();//关闭会话
            session.close();// 关闭会话
            connection.close();//关闭连接
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void updateOrderStatusById(String orderId, String status) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        orderInfo.setOrderStatus(status);
        orderInfoMapper.updateByPrimaryKey(orderInfo);
    }
}
