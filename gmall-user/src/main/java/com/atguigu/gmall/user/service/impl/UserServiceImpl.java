package com.atguigu.gmall.user.service.impl;


import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.UserAddress;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.service.UserService;
import com.atguigu.gmall.user.mapper.UserAddressMapper;
import com.atguigu.gmall.user.mapper.UserInfoMapper;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

/**
 * @param
 * @return
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    UserInfoMapper userInfoMapper;

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    UserAddressMapper userAddressMapper;

    private String userKey_prefix="user:";
    private String userinfoKey_suffix=":info";
    private int userinfo_expire=60*60;

    public List<UserInfo> getUserInfoListAll(){
        List<UserInfo> userInfos = userInfoMapper.selectAll();
        UserInfo userinfoQuery =new UserInfo();
        userinfoQuery.setLoginName("chenge");
        List<UserInfo> userInfos1 = userInfoMapper.select(userinfoQuery);

        Example example=new Example(UserInfo.class);
        example.createCriteria().andLike("name" ,"张%").andEqualTo("id","3");
        List<UserInfo> userInfos2 = userInfoMapper.selectByExample(example);

        return userInfoMapper.selectAll();
    }

    public void addUser(UserInfo userInfo){
        userInfo.setPasswd(userInfo.getPasswd());
        userInfoMapper.insertSelective(userInfo);
    }


    public void updateUser(String id,UserInfo userInfo){
        Example example=new Example(UserInfo.class);
        example.createCriteria().andLike("name" ,"张%").andEqualTo("id","3");
        userInfoMapper.updateByExampleSelective(userInfo,example);

    }

    public List<UserAddress> getUserAddressList(String userId){

        UserAddress userAddress=new UserAddress();
        userAddress.setUserId(userId);

        List<UserAddress> userAddressList = userAddressMapper.select(userAddress);

        return  userAddressList;
    }


    /***
     * 登陆
     * @param userInfo
     * @return
     */
    public UserInfo login(UserInfo userInfo){

        UserInfo userInfoResult = userInfoMapper.selectOne(userInfo);

        // 登陆时存入redis,设置过期时间
        if(userInfoResult!=null){
            Jedis jedis = redisUtil.getJedis();
            // user:id:info
            jedis.setex("user:"+userInfoResult.getId()+":info",60*30, JSON.toJSONString(userInfoResult));
            jedis.close();
        }


        return userInfoResult;
    }

    /***
     *  认证
     * @param userId
     * @return
     */
    public  UserInfo verify(String userId){

        // 从redis取值
        Jedis jedis = redisUtil.getJedis();
        String userJson = jedis.get("user:" + userId + ":info");

        if(StringUtils.isBlank(userJson)){
            //过期
            jedis.close();
            return null;
        }else{
            // 刷新过期时间
            jedis.expire("user:" + userId + ":info",60*30);
            jedis.close();
            return JSON.parseObject(userJson,UserInfo.class);
        }
    }

    @Override
    public UserAddress getUserAddress(String addressId) {

        UserAddress userAddress = new UserAddress();
        userAddress.setId(addressId);
        userAddress = userAddressMapper.selectOne(userAddress);

        return userAddress;
    }

}
