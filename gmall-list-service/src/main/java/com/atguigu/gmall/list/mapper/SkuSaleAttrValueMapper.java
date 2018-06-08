package com.atguigu.gmall.list.mapper;


import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.bean.SkuSaleAttrValue;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

/**
 * @param
 * @return
 */
public interface SkuSaleAttrValueMapper extends Mapper<SkuSaleAttrValue> {

    List<SkuInfo> selectSkuAttrValueListBySpu(int spuId);

}
