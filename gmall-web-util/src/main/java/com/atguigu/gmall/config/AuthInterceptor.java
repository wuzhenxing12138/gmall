package com.atguigu.gmall.config;

import com.atguigu.gmall.annotation.LoginRequire;
import com.atguigu.gmall.util.CookieUtil;
import com.atguigu.gmall.util.HttpClientUtil;
import com.atguigu.gmall.util.IpUtil;
import com.atguigu.gmall.util.JwtUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@Component
public class AuthInterceptor extends HandlerInterceptorAdapter{

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // 不需要登陆，return true
        HandlerMethod h = (HandlerMethod) handler;
        LoginRequire methodAnnotation = h.getMethodAnnotation(LoginRequire.class);//根据输入的注解的类型，返回方法中的注解对象
        if(methodAnnotation==null){
            return true;
        }

        // 验证用的token
        String token = "";
        // 第一次登陆后的token
        String newToken = request.getParameter("newToken");
        // cookie中的token
        String oldToken = CookieUtil.getCookieValue(request,"gmall-token",true);

        // 根据用户的token不同情况，验证用户的token
        if(StringUtils.isBlank(oldToken)&&StringUtils.isBlank(newToken)&&methodAnnotation.isNeededSuccess()){//从未登陆过
            StringBuffer requestURL = request.getRequestURL();
            response.sendRedirect("//passport.gmall.com:8085/index?ReturnUrl="+requestURL);
            return false;
        }else if(StringUtils.isNotBlank(oldToken)&&StringUtils.isBlank(newToken)){//以前登陆过
            token = oldToken;
        }else if(StringUtils.isBlank(oldToken)&&StringUtils.isNotBlank(newToken)){//新登陆
            token = newToken;
            // 把新token放入cookie
            CookieUtil.setCookie(request,response,"gmall-token",token,60*30,true);
        }else if(StringUtils.isNotBlank(oldToken)&&StringUtils.isNotBlank(newToken)){//以前登陆过，但是过期了
            token = newToken;
            // 把新token放入cookie
            CookieUtil.setCookie(request,response,"gmall-token",token,60*30,true);
        }

        // 携带token，验证token，调用认证中心
        String verify = "fail";
        // token，ip
        verify = HttpClientUtil.doGet("http://passport.gmall.com:8085/verify?token="+token+"&ip="+IpUtil.getIp(request));


        if(verify.equals("fail")&&methodAnnotation.isNeededSuccess()==true){
            // 认证失败，打回认证中心，重新登陆
            StringBuffer requestURL = request.getRequestURL();
            response.sendRedirect("//passport.gmall.com:8085/index?ReturnUrl="+requestURL);
            return false;
        }

        if(verify.equals("success")){
            // 必须登陆，并且认证通过
            // 不是必须登陆，但是认证通过
            Map<String, Object> map = JwtUtil.decode(token, "atguigugmall", IpUtil.getIp(request));
            String userId = map.get("userId").toString();
            String nickName =   map.get("nickName").toString();
            request.setAttribute("userId",userId);
            request.setAttribute("nickName",nickName);

            CookieUtil.setCookie(request,response,"gmall-token",token,60*30,true);
        }

        return true;
    }
}
