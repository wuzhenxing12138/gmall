package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.*;

import java.util.List;

public interface AttrService {
    List<BaseAttrInfo> getAttrList(String catalog3Id);

    List<BaseCatalog1> getCatalog1();

    List<BaseCatalog2> getCatalog2(String catalog1Id);

    List<BaseCatalog3> getCatalog3(String catalog2Id);

    void saveAttr(BaseAttrInfo baseAttrInfo);

    List<BaseAttrInfo> attrInfoList(String spuId);

    List<SpuSaleAttr> saleAttrValueList(String spuId);

    List<SpuSaleAttr>  spuSaleAttrListCheckBySku(String skuId);

    List<BaseAttrInfo> getAttrListByValueIds(List<String> list);

    String getValueNameById(String valueId);
}
