package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.PaymentInfo;

public interface PaymentService {
    void savePaymentInfo(PaymentInfo paymentInfo);

    PaymentInfo getPaymentInfo(PaymentInfo paymentInfoQuery);

    void updatePaymentInfo(String outTradeNo, PaymentInfo paymentInfo);

    void sendPaymentResult(String out_trade_no, String trade_success);

    void sendDelayPaymentResult(String outTradeNo, int i);

    String checkPaymentStatus(String out_trade_no);

    boolean getIfPaid(String trade_no, String trade_success);
}
