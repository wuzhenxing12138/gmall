package com.atguigu.gmall.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;

@Controller
public class IndexController {

    @RequestMapping("index")
    public String index(Model model ,HttpSession session){

        String str = "hello world!";

        model.addAttribute("hello",str);

        ArrayList<String>  list = new ArrayList<String>();

        for (int i = 0 ;i <5 ;i++){
            list.add("数据"+i);
        }
        model.addAttribute("list",list);

        session.setAttribute("s","im from session");
        return "index";
    }
}
