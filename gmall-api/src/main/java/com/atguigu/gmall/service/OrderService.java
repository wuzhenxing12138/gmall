package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.OrderInfo;

public interface OrderService {

    boolean checkTradeCode(String userId, String tradeCode);

    String genTradeCode(String userId);

    OrderInfo saveOrder(OrderInfo orderInfo);

    OrderInfo getOrderById(String orderId);

    String seletTestOutTradeNo();

    void updateOrderStatus(String out_trade_no, String trade_status);

    OrderInfo getOrderByOutTradeNo(String out_trade_no);

    void sendOrderResult(String out_trade_no, String trade_status);

    void updateOrderStatusById(String orderId, String status);
}
