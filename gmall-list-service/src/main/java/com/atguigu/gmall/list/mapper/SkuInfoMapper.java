package com.atguigu.gmall.list.mapper;

import com.atguigu.gmall.bean.SkuInfo;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

/**
 * @param
 * @return
 */
public interface SkuInfoMapper extends Mapper<SkuInfo> {


    List<SkuInfo> selectSkuInfoListBySpu(int spuId);
}
