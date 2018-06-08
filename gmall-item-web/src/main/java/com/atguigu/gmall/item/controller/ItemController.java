package com.atguigu.gmall.item.controller;


import com.alibaba.fastjson.JSON;
import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.BaseSaleAttr;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.bean.SkuSaleAttrValue;
import com.atguigu.gmall.bean.SpuSaleAttr;
import com.atguigu.gmall.service.AttrService;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.service.SpuService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ItemController {

    @Reference
    SkuService skuService;

    @Reference
    SpuService spuService;

    @Reference
    AttrService attrService;

    @RequestMapping("index")
    public String index(){
        return "item";
    }

    @RequestMapping("/{skuId}.html")
    public String getSku(@PathVariable String skuId, ModelMap map){

        SkuInfo skuInfo = skuService.getSkuById(skuId);
        map.put("skuInfo",skuInfo);

        if(skuInfo==null){
            return "item";
        }

        // 销售属性列表
        List<SpuSaleAttr> listAttr =  attrService.spuSaleAttrListCheckBySku(skuId);
        map.put("spuSaleAttrListCheckBySku",listAttr);

        // 兄弟姐妹列表
        String spuId = spuService.getSpuBySku(skuId);
        List<SkuInfo> skuInfos = skuService.spuSaleAttrListCheckBySku(Integer.parseInt(spuId));

        // 拼接sku列表的hash表，放到页面
        // 61|71|81 : 78
        // 颜色/容量/版本 ：skuId
        // 页面hash表的sku的key的顺序需要跟销售属性列表的顺序一直，方便查找sku的hash表

        //[{"61|71|81":"78"},{"61|71|82":"81"},{"62|71|81":"79"},{"61|72|81":"80"}]
        //{"65|66|69|72":"72","64|68|69|72":"73","63|66|69|72":"18","65|68|70|73":"35","64|66|69|72":"74","65|68|70|72":"25","63|66|71|72":"26","64|67|71|73":"24","65|68|71|74":"19","63|66|70|74":"17"}

        Map<String,String> valuesMap = new HashMap<>();

        for(int i = 0;i<skuInfos.size();i++){
            String sku_id=skuInfos.get(i).getId();
            String valueKey = "";
            List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfos.get(i).getSkuSaleAttrValueList();

            for(int j=0;j<skuSaleAttrValueList.size();j++){
                if(j>0){
                    valueKey = valueKey +"|";
                }
                valueKey= valueKey + skuSaleAttrValueList.get(j).getSaleAttrValueId();
            }

            valuesMap.put(valueKey,sku_id);
        }

        String json = JSON.toJSONString(valuesMap);
        System.out.println(json);

        map.put("valuesMap",json);
        map.put("sku_id",skuInfo.getId());

        return "item";
    }
}
