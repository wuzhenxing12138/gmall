package com.atguigu.gmall.payment.controller;


import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.atguigu.gmall.annotation.LoginRequire;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PaymentController {

    @Reference
    OrderService orderService;

    @Autowired
    AlipayClient alipayClient;

    @Autowired
    PaymentService paymentService;


    // notify 支付宝的通知接口，更新支付状态，返回success
    @RequestMapping("/alipay/callback/notify")
    @ResponseBody
    public String alipayCallbackNotify(){
        // notify接口的任务
        // 返回支付状态
        // 返回支付信息
        // 更新支付信息表
        // 发送订单支付成功的消息通知，异步启动订单系统处理已支付订单
        // 返回给支付宝success
        Map<String, String> paramsMap =null;// ... //将异步通知中收到的所有参数都存放到map中
        try {
            boolean signVerified = AlipaySignature.rsaCheckV1(paramsMap, AlipayConfig.alipay_public_key, AlipayConfig.charset, AlipayConfig.sign_type); //调用SDK验证签名
            if(signVerified){
                // TODO 验签成功后，按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure
            }else{
                // TODO 验签失败则记录异常日志，并在response中返回failure.
            }
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }

        return null;
    }

    @LoginRequire(isNeededSuccess = true)
    @RequestMapping("/alipay/callback/return")
    public String alipayCallbackReturn(HttpServletRequest request){
        String outTradeNo = orderService.seletTestOutTradeNo();
        String out_trade_no = request.getParameter("out_trade_no");//支付宝调用参数
        String trade_no = request.getParameter("trade_no");//支付宝调用参数
        // 用out_trade_no检查一次当前的out_trade_no对应的支付状态，如果为不是已支付状态，才继续修改状态，发送消息，
        boolean ifPaid = paymentService.getIfPaid(out_trade_no, "TRADE_SUCCESS");

        if(!ifPaid){
            // 浏览器重定向的url，没有支付宝的参数
            Map<String,String> paramsMap = new HashMap<String,String>();

            paramsMap.put("out_trade_no",outTradeNo);
            try {
                // 校验签名的正确，验证支付宝身份
                boolean signVerified = AlipaySignature.rsaCheckV1(paramsMap, AlipayConfig.alipay_public_key, AlipayConfig.charset, AlipayConfig.sign_type); //调用SDK验证签名
                System.out.println(signVerified);
            } catch (AlipayApiException e) {
                e.printStackTrace();
            }

            // notify接口的任务
            // 返回支付状态
            // 返回支付信息
            // 更新支付信息表

            PaymentInfo paymentInfo = new PaymentInfo();
            paymentInfo.setAlipayTradeNo(System.currentTimeMillis()+"");
            paymentInfo.setCallbackTime(new Date());
            paymentInfo.setCallbackContent("支付宝的返回的参数的json字符串");
            paymentInfo.setPaymentStatus("已支付");
            paymentService.updatePaymentInfo(outTradeNo,paymentInfo);

            // 1 触发支付成功消息
            // 发送订单支付成功的消息通知，异步启动订单系统处理已支付订单
            paymentService.sendPaymentResult(out_trade_no,"TRADE_SUCCESS");//payment_result_queue
            // 返回给支付宝success
        }

        return "redirect:"+AlipayConfig.return_order_url;
    }

    /***
     * 提交支付
     * @param request
     * @param map
     * @return
     */
    @LoginRequire(isNeededSuccess = true)
    @RequestMapping("/alipay/submit")
    @ResponseBody
    public String alipaySubmit(HttpServletRequest request, ModelMap map){
        String orderId = request.getParameter("orderId");
        OrderInfo orderInfo = orderService.getOrderById(orderId);
        // 制作支付宝的请求
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();//创建API对应的request
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url);//在公共参数中设置回跳和通知地址
        //out_trade_no
        //product_code
        //total_amount
        //subject
        Map<String,Object> alipayParamap = new HashMap<String,Object>();
        alipayParamap.put("out_trade_no",orderInfo.getOutTradeNo());
        alipayParamap.put("product_code","FAST_INSTANT_TRADE_PAY");
        alipayParamap.put("total_amount",0.01);//orderInfo.getTotalAmount()
        alipayParamap.put("subject",orderInfo.getOrderDetailList().get(0).getSkuName());
        alipayRequest.setBizContent(JSON.toJSONString(alipayParamap));//填充业务参数
        String form="";
        try {
            form = alipayClient.pageExecute(alipayRequest).getBody(); //调用SDK生成表单
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }

        // 保存该笔交易支付信息
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setPaymentStatus("支付中");
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setOrderId(orderId);
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setSubject(orderInfo.getOrderDetailList().get(0).getSkuName());
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());
        paymentService.savePaymentInfo(paymentInfo);

        // 2 触发查询支付成功的定时任务，来检测支付结果(mq延迟队列)
        paymentService.sendDelayPaymentResult(orderInfo.getOutTradeNo(),5);

        return form;
    }

    @LoginRequire(isNeededSuccess = true)
    @RequestMapping("index")
    public String index(HttpServletRequest request, ModelMap map){
        String orderId = request.getParameter("orderId");
        String userId = request.getAttribute("userId").toString();
        String nickName = request.getAttribute("nickName").toString();
        OrderInfo orderInfo = orderService.getOrderById(orderId);

        map.put("orderId",orderId);
        map.put("nickName",nickName);
        map.put("totalAmount",orderInfo.getTotalAmount());
        return "paymentindex";
    }
}
