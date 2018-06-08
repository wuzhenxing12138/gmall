package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.service.AttrService;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.service.SpuService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@Controller
public class SkuController {

    @Reference
    SkuService skuService;

    @Reference
    SpuService spuService;

    @Reference
    AttrService attrService;

    @RequestMapping("saveSku")
    @ResponseBody
    public String saveSku(SkuInfo skuInfo) {
        skuService.saveSkuInfo(skuInfo);

        return "success";
    }

    @RequestMapping("saleAttrValueList")
    @ResponseBody
    public List<SpuSaleAttr> saleAttrValueList(@RequestParam Map<String,String> map) {
        String spuId = map.get("spuId");
        List<SpuSaleAttr>  list = attrService.saleAttrValueList(spuId);
        return list;
    }


    @RequestMapping("attrInfoList")
    @ResponseBody
    public List<BaseAttrInfo> attrInfoList(@RequestParam Map<String,String> map) {
        String catalog3Id = map.get("catalog3Id");
        List<BaseAttrInfo>  list = attrService.attrInfoList(catalog3Id);
        return list;
    }

    @RequestMapping("getSpuImageList")
    @ResponseBody
    public List<SpuImage> getSpuImageList(@RequestParam Map<String,String> map) {
        String spuId = map.get("spuId");


        List<SpuImage>  list = spuService.getSpuImageList(spuId);

        return list;
    }

    @RequestMapping("skuInfoListBySpu")
    @ResponseBody
    public List<SkuInfo> skuInfoListBySpu(@RequestParam Map<String,String> map) {
        String spuId = map.get("spuId");


        List<SkuInfo>  listSkuInfo = skuService.skuInfoListBySpu(spuId);

        return listSkuInfo;
    }
}
