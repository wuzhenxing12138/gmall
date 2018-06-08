package com.atguigu.gmall.cart.util;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.util.CookieUtil;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CartCookieHandler {

    /***
     * 添加购物车
     * @param request
     * @param response
     * @param cartInfo
     */
    public static void addToCart(HttpServletRequest request, HttpServletResponse response, CartInfo cartInfo) {

        String ifCartCookie = CookieUtil.getCookieValue(request, "cart-gmall", true);
        List<CartInfo> cartInfos = new ArrayList<>();

        if(StringUtils.isNotBlank(ifCartCookie)){
            cartInfos = JSON.parseArray(ifCartCookie,CartInfo.class);
            boolean b = ifNewCart(cartInfos,cartInfo);
            if(b){
                // 新车，添加
                cartInfos.add(cartInfo);
            }else{
                // 老车，更新
                //循环cartInfos，根据skuId，修改数量和购物车合计
                for (CartInfo info : cartInfos) {
                    if(info.getSkuId().equals(cartInfo.getSkuId())){
                        info.setSkuNum(info.getSkuNum()+cartInfo.getSkuNum());
                        info.setCartPrice(info.getSkuPrice().multiply(new BigDecimal(info.getSkuNum())));
                    }
                }
            }
        }else{
            // cookie未空，直接添加一个购物车对象到cookie的购物车集合中
            cartInfos.add(cartInfo);
        }
        CookieUtil.setCookie(request,response,"cart-gmall",JSON.toJSONString(cartInfos),60*60*24*7,true);
    }

    /***
     * 判断是否是添加过的购物车对象
     * @param cartInfos
     * @param cartInfo
     * @return
     */
    private static boolean ifNewCart(List<CartInfo> cartInfos, CartInfo cartInfo) {

        boolean b = true;

        for (CartInfo info : cartInfos) {
            String skuId = info.getSkuId();
            if(skuId.equals(cartInfo.getSkuId())){
                b = false;
            }
        }

        return b;
    }

    /***
     * 查询购物车列表
     * @param request
     * @return
     */
    public static List<CartInfo> getCartCookieList(HttpServletRequest request) {

        String cookieValue = CookieUtil.getCookieValue(request, "cart-gmall", true);

        List<CartInfo> cartCookieList = JSON.parseArray(cookieValue, CartInfo.class);

        return cartCookieList;
    }

    /***
     * 购物车选择
     * @param request
     * @param response
     * @param cartCookieList
     * @param cartInfo
     */
    public static void checkCart(HttpServletRequest request, HttpServletResponse response, List<CartInfo> cartCookieList, CartInfo cartInfo) {

        for (CartInfo info : cartCookieList) {
            if(info.getSkuId().equals(cartInfo.getSkuId())){
                info.setIsChecked(cartInfo.getIsChecked());
            }
        }

        // 把修改后的购物车cookie放回浏览器
        CookieUtil.setCookie(request,response,"cart-gmall",JSON.toJSONString(cartCookieList),60*60*24*7,true);

    }
}
