package com.atguigu.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.annotation.LoginRequire;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.cart.util.CartCookieHandler;
import com.atguigu.gmall.service.CartService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Controller
public class CartController {

    @Reference
    CartService cartService;

    /***
     * 购物车选中
     * @param request
     * @param response
     * @param map
     * @return
     */
    @LoginRequire(isNeededSuccess = false)
    @RequestMapping("checkCart")
    public String checkCart(HttpServletRequest request , HttpServletResponse response, ModelMap map)
    {
        String userId = (String)request.getAttribute("userId");
        String nickName = (String)request.getAttribute("nickName");
        String skuId = (String)request.getParameter("skuId");
        String isChecked = (String)request.getParameter("isChecked");
        CartInfo cartInfo = new CartInfo();
        cartInfo.setSkuId(skuId);
        cartInfo.setIsChecked(isChecked);

        List<CartInfo> cartCookieList = new ArrayList<>();
        if(StringUtils.isBlank(userId)){
            // 用户未登陆

            // 修改cookie中的购物车
            cartCookieList =  CartCookieHandler.getCartCookieList(request);

            CartCookieHandler.checkCart(request,response,cartCookieList,cartInfo);
        }else{
            // 用户登陆
            cartInfo.setUserId(userId);
            cartService.checkCart(cartInfo);
            // 页面需要的数据
            cartCookieList = cartService.cartList(userId);
        }

        BigDecimal totalPrice = new BigDecimal("0");
        if(cartCookieList!=null&&cartCookieList.size()>0){
            totalPrice = getTotalPrice(cartCookieList);
        }
        map.put("totalPrice",totalPrice);
        map.put("cartList",cartCookieList);
        return "cartListInner";
    }
    /***
     * 购物车列表
     * @param request
     * @param response
     * @param map
     * @return
     */
    @LoginRequire(isNeededSuccess = false)
    @RequestMapping("cartList")
    public String cartList(HttpServletRequest request , HttpServletResponse response, ModelMap map)
    {
        List<CartInfo> cartList = new ArrayList<>();
        String userId = (String)request.getAttribute("userId");
        String nickName = (String)request.getAttribute("nickName");
        if(StringUtils.isBlank(userId)){
            // 用户未登陆
            cartList =  CartCookieHandler.getCartCookieList(request);
        }else{
            // 用户登陆
            cartList = cartService.cartList(userId);
        }

        BigDecimal totalPrice = new BigDecimal("0");
        if(cartList!=null&&cartList.size()>0){

             totalPrice = getTotalPrice(cartList);
        }
        map.put("totalPrice",totalPrice);
        map.put("cartList",cartList);
        return "cartList";
    }


    /****
     * 添加购物车
     * @param request
     * @param response
     * @param map
     * @return
     */
    @LoginRequire(isNeededSuccess = false)
    @RequestMapping("addToCart")
    public String addToCart(HttpServletRequest request , HttpServletResponse response, ModelMap map){
        // 参数的封装
        String userId = (String)request.getAttribute("userId");
        String nickName = (String)request.getAttribute("nickName");
        String skuId = (String)request.getParameter("skuId");
        String num = (String)request.getParameter("num");
        String price = (String)request.getParameter("price");
        String skuName = (String)request.getParameter("skuName");
        String imgUrl = (String)request.getParameter("imgUrl");
        CartInfo cartInfo = new CartInfo();
        cartInfo.setSkuId(skuId);
        cartInfo.setSkuNum(Integer.parseInt(num));
        cartInfo.setSkuPrice(new BigDecimal(price));
        cartInfo.setCartPrice(cartInfo.getSkuPrice().multiply(new BigDecimal(cartInfo.getSkuNum())));
        cartInfo.setImgUrl(imgUrl);
        cartInfo.setSkuName(skuName);
        cartInfo.setIsChecked("1");

        // 购物车添加
        if(StringUtils.isBlank(userId)){
            // 用户未登陆，使用cookie
            CartCookieHandler.addToCart(request,response,cartInfo);
        }else{
            //用户已登陆，使用db和redis
            cartInfo.setUserId(userId);
            cartService.addToCart(cartInfo);
        }

        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setSkuName(cartInfo.getSkuName());
        skuInfo.setId(cartInfo.getSkuId());
        skuInfo.setSkuDefaultImg(cartInfo.getImgUrl());

        map.put("skuInfo",skuInfo);
        map.put("skuNum",cartInfo.getSkuNum());
        return "success";
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
