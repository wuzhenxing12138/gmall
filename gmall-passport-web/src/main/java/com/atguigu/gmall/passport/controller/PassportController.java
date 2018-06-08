package com.atguigu.gmall.passport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.UserService;
import com.atguigu.gmall.util.CookieUtil;
import com.atguigu.gmall.util.IpUtil;
import com.atguigu.gmall.util.JwtUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PassportController {

    @Reference
    UserService userService;


    @Reference
    CartService cartService;

    @RequestMapping("index")
    public String index(String ReturnUrl, ModelMap map){
        map.put("ReturnUrl",ReturnUrl);
        return "index";
    }

    @RequestMapping("login")
    @ResponseBody
    public String login(HttpServletRequest request, HttpServletResponse response){
        String gmallToken = "";
        // 验证用户名和密码

        // 验证用户和密码，需要访问用户服务
        String loginName = request.getParameter("loginName");
        String passwd = request.getParameter("passwd");
        UserInfo userInfo = new UserInfo();
        userInfo.setLoginName(loginName);
        userInfo.setPasswd(passwd);
        UserInfo login = userService.login(userInfo);

        if(login==null){
            //用户或者密码错误
        }else{
            // 用jwt生成证书token
            // 颁发证书
            Map<String,Object> map = new HashMap<String,Object>();

            map.put("userId",login.getId());
            map.put("nickName",login.getNickName());
            //获得客户端的ip
            String ip = IpUtil.getIp(request);
            gmallToken = JwtUtil.encode("atguigugmall", map, ip);

            // 调用合并购物车的服务
            String cookieValue = CookieUtil.getCookieValue(request, "cart-gmall", true);
            cartService.mergeToCart(JSON.parseArray(cookieValue, CartInfo.class),login);

            // 清理当前浏览器中cookie的数据
            CookieUtil.deleteCookie(request,response,"cart-gmall");

        }

        return gmallToken;
    }

    @RequestMapping("verify")
    @ResponseBody
    public String verify(HttpServletRequest request){
        String token = request.getParameter("token");
        String ip = request.getParameter("ip");

        if(StringUtils.isNotBlank(token)){
            Map<String, Object> atguigugmall = JwtUtil.decode(token, "atguigugmall", ip);
            // token假的
            if(atguigugmall==null){
                return "fail";
            }

            // token是否过期
            String userId = atguigugmall.get("userId").toString();
            UserInfo userInfo = userService.verify(userId);
            if(userInfo==null){
                return "fail";
            }

        }else{
            return "fail";
        }

        return "success";
    }

}
