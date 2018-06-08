package com.atguigu.gmall.list.test;


import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.SkuAttrValue;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.bean.SkuLsInfo;
import com.atguigu.gmall.list.mapper.SkuAttrValueMapper;
import com.atguigu.gmall.list.mapper.SkuImageMapper;
import com.atguigu.gmall.list.mapper.SkuInfoMapper;
import com.atguigu.gmall.list.mapper.SkuSaleAttrValueMapper;
import com.atguigu.gmall.util.RedisUtil;
import io.searchbox.client.JestClient;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class Test1 {

    @Autowired
    SkuInfoMapper skuInfoMapper;
    @Autowired
    SkuImageMapper skuImageMapper;
    @Autowired
    SkuAttrValueMapper skuAttrValueMapper;
    @Autowired
    SkuSaleAttrValueMapper skuSaleAttrValueMapper;
    @Autowired
    JestClient jestClient;


    @Test
    public void b (){

//        searchTools.query();
//        searchTools.from();
//        searchTools.size();
//        searchTools.highlight();
//        searchTools.sort();
//        searchTools.aggregation();

        // 封装查询语句
        SearchSourceBuilder  searchTools = new SearchSourceBuilder();

        BoolQueryBuilder bool = new BoolQueryBuilder();

        // 过滤条件
        TermsQueryBuilder term1 = new TermsQueryBuilder("catalog3Id","61");
        String[] valueIds = new String[2];
        valueIds[0]="43";
        valueIds[1]="48";
        TermsQueryBuilder term2 = new TermsQueryBuilder("skuAttrValueList.valueId",valueIds);
        bool.filter(term1);
        bool.filter(term2);

        // 查询条件
        MatchQueryBuilder match1 = new MatchQueryBuilder("skuName","小米大米中米老司机");
        bool.must(match1);

        // 放入query条件
        searchTools.query(bool);

        // 排序，分页from和size，高亮
        searchTools.sort("hotScore");
        searchTools.from(0);
        searchTools.size(50);
        HighlightBuilder h = new HighlightBuilder();
        h.field("skuName");
        searchTools.highlight(h);

        String query = searchTools.toString();

        System.out.println(query);

        // 执行查询语句
        Search search = new Search.Builder(query).addIndex("gmall").addType("SkuLsInfo").build();

        // 解析返回结果
        List<SkuLsInfo> list_sku = new ArrayList<SkuLsInfo>();
        try {
            SearchResult execute = jestClient.execute(search);
            List<SearchResult.Hit<SkuLsInfo, Void>> hits = execute.getHits(SkuLsInfo.class);

            for (SearchResult.Hit<SkuLsInfo, Void> hit:hits) {
                SkuLsInfo sku = hit.source;

                list_sku.add(sku);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println(111);
    }

    @Test
    public void a() {

        List<SkuInfo> skuInfos = new ArrayList<SkuInfo>();

        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setCatalog3Id("61");
        skuInfos = skuInfoMapper.select(skuInfo);

        for (SkuInfo sku : skuInfos){
            String id = sku.getId();
            SkuAttrValue skuAttrValue = new SkuAttrValue();
            skuAttrValue.setSkuId(id);
            List<SkuAttrValue> skuAttrValues = skuAttrValueMapper.select(skuAttrValue);

            sku.setSkuAttrValueList(skuAttrValues);
        }
        System.out.println(skuInfos);

        try {
            List<SkuLsInfo> skuLsInfos = new ArrayList<SkuLsInfo>();

            for (SkuInfo sku : skuInfos){
                SkuLsInfo skuLs = new SkuLsInfo();
                BeanUtils.copyProperties(skuLs,sku);
                skuLsInfos.add(skuLs);
            }

            // 打印对象的json
            System.out.println(JSON.toJSONString(skuLsInfos));

            for (SkuLsInfo skuLsInfo:skuLsInfos) {


                // jest的增加对象
                Index index = new Index.Builder(skuLsInfo).index("gmall").type("SkuLsInfo").id(skuLsInfo.getId()).build();

                try {
                    jestClient.execute(index);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }



        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }


    }
}
