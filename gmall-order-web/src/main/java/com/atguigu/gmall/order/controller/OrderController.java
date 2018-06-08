package com.atguigu.gmall.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.annotation.LoginRequire;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.bean.enums.OrderStatus;
import com.atguigu.gmall.bean.enums.ProcessStatus;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Controller
public class OrderController {

    @Reference
    UserService userService;

    @Reference
    CartService cartService;

    @Reference
    OrderService orderService;

    @Reference
    SkuService skuService;

    @LoginRequire(isNeededSuccess = true)
    @RequestMapping("list")
    public String list(){
        return "list";
    }

    @LoginRequire(isNeededSuccess = true)
    @RequestMapping("submitOrder")
    public String submitOrder(HttpServletRequest request , HttpServletResponse response, String addressId,ModelMap map){
        String userId = request.getAttribute("userId").toString();
        String nickName = request.getAttribute("nickName").toString();
        String tradeCode = request.getParameter("tradeCode");

        boolean b = orderService.checkTradeCode(userId,tradeCode);

        if(!b){
            map.put("errMsg","结算失效，或者订单重复提交");
            return "tradeFail";
        }

        // 正常的表单提交
        List<CartInfo> cartInfos = cartService.cartListByCheck(userId);
        // 获得收获信息
        UserAddress userAddress = userService.getUserAddress(addressId);

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setUserId(userId);
        orderInfo.setConsignee(userAddress.getConsignee());
        orderInfo.setConsigneeTel(userAddress.getPhoneNum());
        orderInfo.setCreateTime(new Date());
        orderInfo.setDeliveryAddress(userAddress.getUserAddress());
        // 过期时间=当前天数+1天
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DATE,1);
        orderInfo.setExpireTime(c.getTime());
        orderInfo.setOrderComment("该订单非常重要 ，请务必送达");
        orderInfo.setOrderStatus(OrderStatus.UNPAID.getComment());
        orderInfo.setProcessStatus(ProcessStatus.UNPAID.getComment());
        // 生成外部单号的规则
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String format = sdf.format(new Date());
        String outTradeNo = "ATGUIGU"+System.currentTimeMillis()+format;
        orderInfo.setOutTradeNo(outTradeNo);//对外部系统的统一业务编号
        orderInfo.setTotalAmount(getTotalPrice(cartInfos));

        // 保存订单详情
        List<OrderDetail> orderDetailList = new ArrayList<>();

        for (CartInfo cartInfo : cartInfos) {
            // 验价
            BigDecimal cartPrice = cartInfo.getSkuPrice();
            String skuId = cartInfo.getSkuId();
            SkuInfo skuByIdFromDb = skuService.getSkuByIdFromDb(skuId);
            BigDecimal skuPrice = skuByIdFromDb.getPrice();
            int i = cartPrice.compareTo(skuPrice);
            if(i!=0){
                map.put("errMsg","商品价格发生变化，请重新确认订单");
                return "tradeFail";
            }
            // 验库存
            // 通过远程rpc的协议访问库存系统，来检验当前库存数量是否大于购买数量
            // HttpClientUtil.doGet();
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setSkuNum(cartInfo.getSkuNum());
            orderDetail.setSkuName(cartInfo.getSkuName());
            orderDetail.setSkuId(cartInfo.getSkuId());
            orderDetail.setOrderPrice(cartInfo.getCartPrice());
            orderDetail.setImgUrl(cartInfo.getImgUrl());

            orderDetailList.add(orderDetail);
        }

        orderInfo.setOrderDetailList(orderDetailList);

        orderInfo = orderService.saveOrder(orderInfo);

        // 删除购物车
        // cartService.delCartById(cartInfos,userId);

        return "redirect://payment.gmall.com:8087/index?orderId="+orderInfo.getId();
    }

    @RequestMapping("toTrade")
    @LoginRequire(isNeededSuccess = true)
    public String toTrade(HttpServletRequest request , HttpServletResponse response, ModelMap map){

        String userId = request.getAttribute("userId").toString();
        String nickName = request.getAttribute("nickName").toString();

        List<UserAddress> userAddressList = userService.getUserAddressList(userId);

        // 订单列表
        List<OrderDetail> orderDetailList = new ArrayList<>();

        List<CartInfo> cartInfoList = cartService.cartList(userId);

        for (CartInfo cartInfo : cartInfoList) {
            if(cartInfo.getIsChecked().equals("1")){
                OrderDetail orderDetail = new OrderDetail();
                orderDetail.setImgUrl(cartInfo.getImgUrl());
                orderDetail.setOrderPrice(cartInfo.getCartPrice());
                orderDetail.setSkuId(cartInfo.getSkuId());
                orderDetail.setSkuName(cartInfo.getSkuName());
                orderDetail.setSkuNum(cartInfo.getSkuNum());
                orderDetailList.add(orderDetail);
            }
        }

        map.put("nickName",nickName);
        map.put("userAddressList",userAddressList);
        map.put("orderDetailList",orderDetailList);
        map.put("totalAmount",getTotalPrice(cartInfoList));

        String tradeCode = orderService.genTradeCode(userId);
        map.put("tradeCode",tradeCode);
        return "trade";
    }


    /***
     * 计算总金额
     * @param cartCookieList
     * @return
     */
    private BigDecimal getTotalPrice(List<CartInfo> cartCookieList) {
        BigDecimal totalPrice = new BigDecimal("0");
        for (CartInfo cartInfo : cartCookieList) {
            if(cartInfo.getIsChecked().equals("1")){
                totalPrice = totalPrice.add(cartInfo.getCartPrice());
            }
        }
        return totalPrice;
    }

}
