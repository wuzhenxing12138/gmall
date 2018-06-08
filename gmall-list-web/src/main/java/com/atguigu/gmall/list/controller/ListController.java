package com.atguigu.gmall.list.controller;


import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.service.AttrService;
import com.atguigu.gmall.service.ListService;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.*;

@Controller
public class ListController {

    @Reference
    ListService listService;


    @Reference
    AttrService attrService;

    @RequestMapping("list.html")
    public String list(SkuLsParam skuLsParam, ModelMap map){

        List<SkuLsInfo> skuLsInfoList = listService.search(skuLsParam);

        // 根据检索到的sku所包含的属性值集合，到数据库将属性/属性值的页面列表检索出来
        List<BaseAttrInfo> attrList = getAttrList(skuLsInfoList,skuLsParam.getValueId());// 参数：属性值id的集合

        // 当前请求的url
        String urlParam = getUrl(skuLsParam,null);

        // 制作面包屑
        List<Crumb> crumbs = new ArrayList<Crumb>();
        String[] valueIds = skuLsParam.getValueId();
        if (valueIds != null) {
            for (int i = 0; i <valueIds.length ; i++) {
                Crumb crumb = new Crumb();
                String valueId =  valueIds[i];
                String valueName = attrService.getValueNameById(valueId);
                String url =  getUrl(skuLsParam,valueId);

                crumb.setValueName(valueName);
                crumb.setUrlParam(url);
                crumbs.add(crumb);
            }
        }


        map.put("attrValueSelectedList",crumbs);
        map.put("skuLsInfoList",skuLsInfoList);
        map.put("attrList",attrList);
        map.put("urlParam",urlParam);

        return "list";
    }

    /****
     * 制作请求url
     * @param skuLsParam
     * @return
     */
    private String getUrl(SkuLsParam skuLsParam,String valueId) {

        String url = "";

        String keyword = skuLsParam.getKeyword();

        String[] valueIds = skuLsParam.getValueId();

        String catalog3Id = skuLsParam.getCatalog3Id();

        if(StringUtils.isNotBlank(keyword)){
            if(StringUtils.isNotBlank(url)){
                url = url+"&";
            }
            url = url+"keyword="+keyword;
        }

        if(StringUtils.isNotBlank(catalog3Id)){
            if(StringUtils.isNotBlank(url)){
                url = url+"&";
            }
            url = url+"catalog3Id="+catalog3Id;
        }

        if(valueIds!=null&&valueIds.length>0){
            for (int i = 0 ;i<valueIds.length;i++){
                if(valueId==null){
                    url = url+"&";
                    url = url+"valueId="+valueIds[i];
                }else{
                    if(!valueId.equals(valueIds[i])){
                        url = url+"&";
                        url = url+"valueId="+valueIds[i];
                    }
                }
            }
        }

        return url;
    }

    /***
     *
     * @param skuLsInfoList
     * @param valueIds
     * @return
     */
    private List<BaseAttrInfo> getAttrList(List<SkuLsInfo> skuLsInfoList, String[] valueIds) {

        // ifn
        // inn
        // fori
        // object.for

        // 获得去重后的sku列表中的属性值集合
        Set<String> setValueId = new HashSet<String>();
        for (int i =0;i<skuLsInfoList.size();i++){
            SkuLsInfo skuLsInfo = skuLsInfoList.get(i);
            List<SkuLsAttrValue> skuAttrValueList = skuLsInfo.getSkuAttrValueList();
            for (SkuLsAttrValue skuLsAttrValue:skuAttrValueList) {
                setValueId.add(skuLsAttrValue.getValueId());
            }
        }
        List<String> list = new ArrayList<>();
        list.addAll(setValueId);

        List<BaseAttrInfo> attrList = attrService.getAttrListByValueIds(list);

        // 在属性列表中去掉已经选择的属性
        if(valueIds!=null&&valueIds.length>0){
            List<String> removeList = new ArrayList<>();
            for (int i =0 ;i<valueIds.length;i++){
                removeList.add(valueIds[i]);
            }
            Iterator<BaseAttrInfo> iterator = attrList.iterator();
            while(iterator.hasNext()){
                BaseAttrInfo next = iterator.next();
                List<BaseAttrValue> attrValueList = next.getAttrValueList();
                for (BaseAttrValue baseAttrValue:attrValueList) {
                    String id = baseAttrValue.getId();
                    boolean b = removeList.contains(id);
                    if(b){
                        iterator.remove();
                    }
                }
            }
        }
        return attrList;
    }
}
