package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.SkuAttrValue;
import com.atguigu.gmall.bean.SkuImage;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.bean.SkuSaleAttrValue;
import com.atguigu.gmall.manage.mapper.SkuAttrValueMapper;
import com.atguigu.gmall.manage.mapper.SkuImageMapper;
import com.atguigu.gmall.manage.mapper.SkuInfoMapper;
import com.atguigu.gmall.manage.mapper.SkuSaleAttrValueMapper;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

@Service
public class SkuServiceImpl implements SkuService {


    @Autowired
    SkuInfoMapper skuInfoMapper;
    @Autowired
    SkuImageMapper skuImageMapper;
    @Autowired
    SkuAttrValueMapper skuAttrValueMapper;
    @Autowired
    SkuSaleAttrValueMapper skuSaleAttrValueMapper;

    @Autowired
    RedisUtil redisUtil;

    @Override
    public List<SkuInfo> skuInfoListBySpu(String spuId) {

        List<SkuInfo> list = skuInfoMapper.selectSkuInfoListBySpu(Integer.parseInt(spuId));

        return list;
    }

    @Override
    public SkuInfo saveSkuInfo(SkuInfo skuInfo){

        // 根据id判断保存或者更新
        if(skuInfo.getId()==null||skuInfo.getId().length()==0){
            skuInfo.setId(null);
            skuInfoMapper.insertSelective(skuInfo);
        }else {
            skuInfoMapper.updateByPrimaryKeySelective(skuInfo);
        }


        // 删除图片
        Example example=new Example(SkuImage.class);
        example.createCriteria().andEqualTo("skuId",skuInfo.getId());
        skuImageMapper.deleteByExample(example);
//        SkuImage s = new SkuImage();
//        s.setSkuId(skuInfo.getId());
//        skuImageMapper.delete();
        // 保存图片
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        for (SkuImage skuImage : skuImageList) {
            skuImage.setSkuId(skuInfo.getId());
            skuImage.setId(null);
            skuImageMapper.insertSelective(skuImage);
        }

        // 删除平台属性
        Example skuAttrValueExample=new Example(SkuAttrValue.class);
        skuAttrValueExample.createCriteria().andEqualTo("skuId",skuInfo.getId());
        skuAttrValueMapper.deleteByExample(skuAttrValueExample);
        //保存平台属性
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        for (SkuAttrValue skuAttrValue : skuAttrValueList) {
            skuAttrValue.setSkuId(skuInfo.getId());
            skuAttrValue.setId(null);
            skuAttrValueMapper.insertSelective(skuAttrValue);
        }

        // 删除销售属性
        Example skuSaleAttrValueExample=new Example(SkuSaleAttrValue.class);
        skuSaleAttrValueExample.createCriteria().andEqualTo("skuId",skuInfo.getId());
        skuSaleAttrValueMapper.deleteByExample(skuSaleAttrValueExample);
        //保存 销售属性
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        for (SkuSaleAttrValue skuSaleAttrValue : skuSaleAttrValueList) {
            skuSaleAttrValue.setSkuId(skuInfo.getId());
            skuSaleAttrValue.setId(null);
            skuSaleAttrValueMapper.insertSelective(skuSaleAttrValue);
        }
        String id = skuInfo.getId();

        skuInfo.setId(id);
        return skuInfo;
    }

    /***
     * 从缓存中取出sku数据
     * @param skuId
     * @return
     */
    @Override
    public SkuInfo getSkuById(String skuId) {

        SkuInfo skuInfo = null;
        
        // 查询redis
        Jedis jedis = redisUtil.getJedis();
        String s = jedis.get("sku:" + skuId + ":info");
        if("empty".equals(s)){
            System.out.println("访问者"+Thread.currentThread().getName()+"发现缓存里没有，数据库中也没有数据，撤。。。");
            return null;
        }else if(StringUtils.isBlank(s)||"null".equals(s)){
            // 当缓存失效，mysql数据库应该如何处理请求
            if(skuInfo==null){
                // 查询mysql
                // SkuConst
                String ok = jedis.set("sku:" + skuId + ":lock", "1", "nx", "px", 10000);

                if(ok.equals("OK")){
                    System.out.println("访问者"+Thread.currentThread().getName()+"发现缓存里没有，拿到缓存锁，访问数据库。。。");
                    // 访问数据库
                    skuInfo = getSkuByIdFromDb(skuId);

                    if(skuInfo==null){
                        // db中也没有
                        System.out.println("访问者"+Thread.currentThread().getName()+"发现缓存里没有，拿到缓存锁，访问数据库后发现数据库中也没有，告诉后面的人一分钟之内不用访问数据库。。。");
                        jedis.setex("sku:" + skuId + ":info",60000,"empty");
                    }else{
                        System.out.println("访问者"+Thread.currentThread().getName()+"发现缓存里没有，拿到缓存锁，从数据库中成功拿到数据。。。");
                        // 保存缓存
                        System.out.println("访问者"+Thread.currentThread().getName()+"发现缓存里没有，拿到缓存锁——将数据库中成功拿到数据保存redis里一份,交还分布式锁。。。");
                        jedis.set("sku:" + skuId + ":info",JSON.toJSONString(skuInfo));
                        jedis.del("sku:" + skuId + ":lock");
                    }

                }else{
                    // 自旋
                    System.out.println("访问者"+Thread.currentThread().getName()+"发现缓存里没有，而且没拿到缓存锁，开始等待自旋。。。");
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    getSkuById(skuId);
                }
            }
        }else{
            System.out.println("访问者"+Thread.currentThread().getName()+"从缓存中拿到数据，成功返回。。。");
            skuInfo = JSON.parseObject(s, SkuInfo.class);// jsonlib
        }

        // 关闭redis
        jedis.close();

        return skuInfo;
    }


    /***
     * 从数据库中查询skuInfo
     * @param skuId
     * @return
     */
    @Override
    public SkuInfo getSkuByIdFromDb(String skuId) {
        SkuInfo skuInfo  = new SkuInfo();
        skuInfo = new SkuInfo();
        skuInfo.setId(skuId);
        skuInfo = skuInfoMapper.selectOne(skuInfo);

        // 数据库中没有该sku
        if(skuInfo == null){
            return null;
        }else{
            SkuImage skuImage = new SkuImage();
            skuImage.setSkuId(skuId);
            List<SkuImage> imgList = skuImageMapper.select(skuImage);
            skuInfo.setSkuImageList(imgList);
        }
        return skuInfo;
    }


    @Override
    public List<SkuInfo> spuSaleAttrListCheckBySku(int spuId){
        return skuSaleAttrValueMapper.selectSkuAttrValueListBySpu(spuId);
    }
}
