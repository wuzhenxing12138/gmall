package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.UserInfo;

import java.util.List;

public interface CartService {
    void addToCart(CartInfo cartInfo);

    List<CartInfo> cartList(String userId);

    void checkCart(CartInfo cartInfo);

    void mergeToCart(List<CartInfo> cartCookieList, UserInfo userInfo);

    List<CartInfo> cartListByCheck(String userId);

    void delCartById(List<CartInfo> cartInfos,String userId);
}
