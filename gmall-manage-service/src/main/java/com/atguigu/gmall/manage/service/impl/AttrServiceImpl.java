package com.atguigu.gmall.manage.service.impl;


import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.manage.mapper.*;
import com.atguigu.gmall.service.AttrService;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AttrServiceImpl implements AttrService {

    @Autowired
    BaseAttrInfoMapper baseAttrInfoMapper;

    @Autowired
    Catalog1Mapper catalog1Mapper;

    @Autowired
    Catalog2Mapper catalog2Mapper;

    @Autowired
    Catalog3Mapper catalog3Mapper;

    @Autowired
    BaseAttrValueMapper baseAttrValueMapper;

    @Autowired
    SpuSaleAttrMapper spuSaleAttrMapper;

    @Autowired
    SpuSaleAttrValueMapper spuSaleAttrValueMapper;

    @Autowired
    SkuInfoMapper skuInfoMapper;

    @Override
    public List<BaseAttrInfo> getAttrList(String catalog3Id) {
        BaseAttrInfo b = new BaseAttrInfo();
        b.setCatalog3Id(catalog3Id);
        List<BaseAttrInfo> select = baseAttrInfoMapper.select(b);
        return select;
    }

    @Override
    public List<BaseCatalog1> getCatalog1() {
        return catalog1Mapper.selectAll();
    }

    @Override
    public List<BaseCatalog2> getCatalog2(String catalog1Id) {
        BaseCatalog2 b = new BaseCatalog2();
        b.setCatalog1Id(catalog1Id);
        return catalog2Mapper.select(b);
    }

    @Override
    public List<BaseCatalog3> getCatalog3(String catalog2Id) {
        BaseCatalog3 b = new BaseCatalog3();
        b.setCatalog2Id(catalog2Id);
        return catalog3Mapper.select(b);
    }

    @Override
    public void saveAttr(BaseAttrInfo baseAttrInfo) {
       String id =  baseAttrInfo.getId();

       if(StringUtils.isBlank(id)){
           // 如果为空添加
           baseAttrInfoMapper.insertSelective(baseAttrInfo);
       }else{
           baseAttrInfoMapper.updateByPrimaryKey(baseAttrInfo);
       }

       // 删掉 所有关联的属性值
        BaseAttrValue baseAttrValue = new BaseAttrValue();
        baseAttrValue.setAttrId(baseAttrInfo.getId());
        baseAttrValueMapper.delete(baseAttrValue);


        // 重新增加最新的属性值内容
        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();

        for (int i = 0; i < attrValueList.size(); i++) {
            BaseAttrValue b = attrValueList.get(i);
            b.setAttrId(baseAttrInfo.getId());
            baseAttrValueMapper.insert(b);
        }
    }

    @Override
    public List<BaseAttrInfo> attrInfoList(String spuId) {

        List<BaseAttrInfo> list =  baseAttrInfoMapper.selectAttrInfoList(Integer.parseInt(spuId));
        return list;
    }

    @Override
    public List<SpuSaleAttr> saleAttrValueList(String spuId) {
        SpuSaleAttr s = new SpuSaleAttr();
        s.setSpuId(spuId);
        List<SpuSaleAttr> spuSaleAttrList = spuSaleAttrMapper.select(s);
        for (SpuSaleAttr spuSaleAttr:spuSaleAttrList){
            String id = spuSaleAttr.getSaleAttrId();
            SpuSaleAttrValue ss = new SpuSaleAttrValue();
            ss.setSaleAttrId(id);
            ss.setSpuId(spuId);
            List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttrValueMapper.select(ss);
            spuSaleAttr.setSpuSaleAttrValueList(spuSaleAttrValueList);
            
        }
        return spuSaleAttrList;
    }

    @Override
    public List<SpuSaleAttr> spuSaleAttrListCheckBySku(String skuId) {

        List<SpuSaleAttr> listAttr = new ArrayList<SpuSaleAttr>();

        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setId(skuId);
        SkuInfo sku = skuInfoMapper.selectOne(skuInfo);

        if(sku==null){
            return null;
        }else{
            Map<Object,Object> map = new HashMap<Object,Object>();
            map.put("skuId",Integer.parseInt(skuId));
            map.put("spuId",Integer.parseInt(sku.getSpuId()));
            listAttr = spuSaleAttrValueMapper.selectSpuSaleAttrListCheckBySku(map);
        }
        return listAttr;
    }

    @Override
    public List<BaseAttrInfo> getAttrListByValueIds(List<String> list) {

        String join = StringUtils.join(list.toArray(), ",");// 43,48,45,60...

        List<BaseAttrInfo> attrList = baseAttrInfoMapper.selectAttrListByValueIds(join);
        
        return attrList;
    }

    @Override
    public String getValueNameById(String valueId) {

        String valueName = baseAttrValueMapper.selectValueNameById(valueId);
        return valueName;
    }


}
