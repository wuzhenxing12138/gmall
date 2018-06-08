package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.SkuInfo;

import java.util.List;

public interface SkuService {
    List<SkuInfo> skuInfoListBySpu(String spuId);

    SkuInfo saveSkuInfo(SkuInfo skuInfo);

    SkuInfo getSkuById(String skuId);

    SkuInfo getSkuByIdFromDb(String skuId);

    List<SkuInfo> spuSaleAttrListCheckBySku(int spuId);
}
