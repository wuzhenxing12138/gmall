package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.BaseAttrInfo;
import com.atguigu.gmall.bean.BaseCatalog1;
import com.atguigu.gmall.bean.BaseCatalog2;
import com.atguigu.gmall.bean.BaseCatalog3;
import com.atguigu.gmall.service.AttrService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@Controller
public class AttrController {

    @Reference
    AttrService attrService;

    @RequestMapping("saveAttr")
    @ResponseBody
    public String saveAttr(BaseAttrInfo baseAttrInfo){

        attrService.saveAttr(baseAttrInfo);
        return "success";
    }

    @RequestMapping("getCatalog3")
    @ResponseBody
    public List<BaseCatalog3> getCatalog3(@RequestParam Map<String,String> map){
        // 远程调用dubbo的service服务
        String catalog2Id = map.get("catalog2Id");
        List<BaseCatalog3> listC3 = attrService.getCatalog3(catalog2Id);

        return listC3;
    }

    @RequestMapping("getCatalog2")
    @ResponseBody
    public List<BaseCatalog2> getCatalog2(@RequestParam Map<String,String> map){
        // 远程调用dubbo的service服务
        String catalog1Id = map.get("catalog1Id");
        List<BaseCatalog2> listC2 = attrService.getCatalog2(catalog1Id);

        return listC2;
    }


    @RequestMapping("getCatalog1")
    @ResponseBody
    public List<BaseCatalog1> getCatalog1(@RequestParam Map<String,String> map){
        // 远程调用dubbo的service服务
        List<BaseCatalog1> listC1 = attrService.getCatalog1();

        return listC1;
    }

    @RequestMapping("getAttrList")
    @ResponseBody
    public List<BaseAttrInfo> getAttrList(@RequestParam Map<String,String> map){
        // 远程调用dubbo的service服务

        String catalog3Id = map.get("catalog3Id");
        List<BaseAttrInfo> listAttr = attrService.getAttrList(catalog3Id);

        return listAttr;
    }


}
