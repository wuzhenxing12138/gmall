package com.atguigu.gmall.test;

import com.atguigu.gmall.util.JwtUtil;
import io.jsonwebtoken.impl.Base64UrlCodec;
import org.apache.commons.lang3.StringUtils;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class TestJwt {

    public static void main(String[] args){

        Map<String,Object> map = new HashMap<String,Object>();

        map.put("userId","boge");
        map.put("nickName","复仇联盟领袖");

        String token = JwtUtil.encode("atguigugmall", map, "127.1.1.2");

        // 登陆认证成功后，由认证中心颁发的token
        System.out.println(token);

        Map<String, Object> atguigugmall = JwtUtil.decode(token, "atguigugmall", "127.1.1.2");

        String nickName = atguigugmall.get("nickName").toString();
        String userId = atguigugmall.get("userId").toString();
        System.out.println(nickName+userId);


        // 私有部分
        String s = StringUtils.substringBetween(token, ".");

        Base64UrlCodec b = new Base64UrlCodec();

        byte[] decode = b.decode(s);

        try {
            String str = new String(decode,"utf-8");
            System.out.println(str);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    }
}
