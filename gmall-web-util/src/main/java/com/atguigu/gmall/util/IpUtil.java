package com.atguigu.gmall.util;

import javax.servlet.http.HttpServletRequest;

public class IpUtil {

    public static String getIp(HttpServletRequest request){
        // 获取盐值ip
        String remoteAddr="";
        if (request.getHeader("x-forwarded-for") == null) {
            remoteAddr = request.getRemoteAddr();
        }else{
            remoteAddr = request.getHeader("x-forwarded-for");
        }
        if (remoteAddr == null) {
            remoteAddr = "127.0.0.1";
        }
        return remoteAddr;
    }
}
