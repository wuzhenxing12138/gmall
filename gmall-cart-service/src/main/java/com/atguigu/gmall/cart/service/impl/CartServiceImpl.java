package com.atguigu.gmall.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.cart.mapper.CartInfoMapper;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    CartInfoMapper cartInfoMapper;

    /***
     * 插入购物车
     * @param cartInfo
     */
    @Override
    public void addToCart(CartInfo cartInfo) {
        // 判断是插入还是更新
        CartInfo cartExists = new CartInfo();
        cartExists.setUserId(cartInfo.getUserId());
        cartExists.setSkuId(cartInfo.getSkuId());
        List<CartInfo> select = cartInfoMapper.select(cartExists);
        if(select==null||select.size()==0){
            // 插入数据库
            System.out.println("插入数据库购物车数据");
            cartInfoMapper.insertSelective(cartInfo);
        }else{
            // 更新数据库
            System.out.println("更新数据库购物车数据");
            cartInfo.setId(select.get(0).getId());
            cartInfo.setSkuNum(cartInfo.getSkuNum()+select.get(0).getSkuNum());
            cartInfo.setCartPrice(cartInfo.getCartPrice().add(select.get(0).getCartPrice()));
            cartInfoMapper.updateByPrimaryKeySelective(cartInfo);
        }

        // 插入/更新缓存
        System.out.println("更新或者插入缓存");
        Jedis jedis = redisUtil.getJedis();
        String cartKey  = "user:"+cartInfo.getUserId()+":cart";
        String key = cartInfo.getSkuId();//如果有，更新，如果没有，插入
        String value = JSON.toJSONString(cartInfo);
        jedis.hset(cartKey,key,value);
        jedis.close();

    }

    /***
     * 查询购物车
     * @param userId
     * @return
     */
    @Override
    public List<CartInfo> cartList(String userId) {
        List<CartInfo> cartList = new ArrayList<>();
        // 优先查询缓存
        Jedis jedis = redisUtil.getJedis();
        String cartKey  = "user:"+userId+":cart";
        List<String> cartRedisList = jedis.hvals(cartKey);
        if(cartRedisList!=null&&cartRedisList.size()>0){
            for (String cartRedis : cartRedisList) {
                CartInfo cartInfo = JSON.parseObject(cartRedis, CartInfo.class);
                cartList.add(cartInfo);
            }
        }else{
            // 查询数据库
            System.out.println("缓存中没有该用户的购物车数据，查询数据库"+userId);
            CartInfo cartInfo = new CartInfo();
            cartInfo.setUserId(userId);
            cartList = cartInfoMapper.select(cartInfo);

            // 将数据库中的数据同步到缓存
            Map<String,String> map = new HashMap<>();
            for (CartInfo info : cartList) {
                String key = info.getSkuId();
                String value = JSON.toJSONString(info);
                map.put(key,value);
                System.out.println("将查询数据库中的购物车集合放入缓存"+info.getSkuId());
            }
            jedis.hmset(cartKey,map);
        }

        jedis.close();
        return cartList;
    }

    /***
     * 更新选中状态
     * @param cartInfo
     */
    @Override
    public void checkCart(CartInfo cartInfo) {
        // 查询主键
        CartInfo cartInfoSelect = new CartInfo();
        cartInfoSelect.setUserId(cartInfo.getUserId());
        cartInfoSelect.setSkuId(cartInfo.getSkuId());
        CartInfo cartInfoUpdate = cartInfoMapper.selectOne(cartInfoSelect);
        // 更新数据库
        cartInfoUpdate.setIsChecked(cartInfo.getIsChecked());
        cartInfoMapper.updateByPrimaryKey(cartInfoUpdate);
        System.out.println("更新数据库的选中状态:"+cartInfoUpdate.getId());

        // 更新缓存
        System.out.println("更新缓存的选中状态："+cartInfoUpdate.getId());
        Jedis jedis = redisUtil.getJedis();
        String cartKey  = "user:"+cartInfo.getUserId()+":cart";
        String key = cartInfo.getSkuId();//如果有，更新，如果没有，插入
        String value = JSON.toJSONString(cartInfoUpdate);
        jedis.hset(cartKey,key,value);
        jedis.close();


    }

    @Override
    public void mergeToCart(List<CartInfo> cartCookieList, UserInfo userInfo) {

        CartInfo cartInfo = new CartInfo();
        cartInfo.setUserId(userInfo.getId());

        // 数据库中的购物车数据
        List<CartInfo> cartDbList = cartInfoMapper.select(cartInfo);

        if(cartCookieList!=null&&cartCookieList.size()>0){
            for (CartInfo cartCookie : cartCookieList) {
                boolean b = ifNewCart(cartDbList, cartCookie);

                if(b) {
                    // 新车，插入数据库
                    cartCookie.setUserId(userInfo.getId());
                    cartInfoMapper.insertSelective(cartCookie);
                    cartDbList.add(cartCookie);
                }else{
                    //老车，更新选中状态，以cookie为准
                    for (CartInfo cartDb : cartDbList) {
                        if(cartDb.getSkuId().equals(cartCookie.getSkuId())){
                            cartDb.setIsChecked(cartCookie.getIsChecked());
                            // 更新一下db
                            cartInfoMapper.updateByPrimaryKey(cartDb);
                        }
                    }
                }

            }
        }

        // redis添加登陆后的用户购物车数据
        Jedis jedis = redisUtil.getJedis();
        HashMap<String, String> map = new HashMap<>();
        for (CartInfo cartDb : cartDbList) {
            map.put(cartDb.getSkuId(),JSON.toJSONString(cartDb));
        }
        jedis.hmset("user:"+userInfo.getId()+":cart",map);
        jedis.close();

    }

    @Override
    public List<CartInfo> cartListByCheck(String userId) {
        List<CartInfo> cartListByCheck = new ArrayList<>();
        List<CartInfo> cartInfos = cartList(userId);

        for (CartInfo cartInfo : cartInfos) {
            if(cartInfo.getIsChecked().equals("1")){
                cartListByCheck.add(cartInfo);
            }
        }

        return cartListByCheck;
    }

    @Override
    public void delCartById(List<CartInfo> cartInfos,String userId) {
        for (CartInfo cartInfo : cartInfos) {
            cartInfoMapper.deleteByPrimaryKey(cartInfo);
        }

        Jedis jedis = redisUtil.getJedis();

        String key = "user:"+userId+":cart";
        String[] delKey = new String[cartInfos.size()];
        for (int i = 0; i < cartInfos.size(); i++) {
            delKey[i] = cartInfos.get(i).getSkuId();
        }
        jedis.hdel(key,delKey);

        jedis.close();

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

}
