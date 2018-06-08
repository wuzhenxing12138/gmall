package com.atguigu.gmall.list.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.SkuLsInfo;
import com.atguigu.gmall.bean.SkuLsParam;
import com.atguigu.gmall.service.ListService;
import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.core.search.aggregation.Aggregation;
import io.searchbox.core.search.aggregation.MetricAggregation;
import io.searchbox.core.search.aggregation.TermsAggregation;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ListServiceImpl implements ListService {

    @Autowired
    JestClient jestClient;

    @Override
    public List<SkuLsInfo> search(SkuLsParam skuLsParam) {

        // 封装查询语句
        SearchSourceBuilder searchTools = new SearchSourceBuilder();

        BoolQueryBuilder bool = new BoolQueryBuilder();

        // 三级分类id
        // 过滤条件
        if(StringUtils.isNotBlank(skuLsParam.getCatalog3Id())){
            TermsQueryBuilder term1 = new TermsQueryBuilder("catalog3Id", skuLsParam.getCatalog3Id());
            bool.filter(term1);
        }

        // 属性值的过滤
        // 过滤条件
        String[] valueIds = skuLsParam.getValueId();
        if(valueIds!=null&&valueIds.length>0){
            for(int i =0 ;i<valueIds.length;i++){
                String valueId = valueIds[i];
                TermsQueryBuilder term = new TermsQueryBuilder("skuAttrValueList.valueId", valueId);
                bool.filter(term);
            }
        }

//        String[] valueIds = new String[2];
//        valueIds[0]="43";
//        valueIds[1]="48";
//        TermsQueryBuilder term2 = new TermsQueryBuilder("skuAttrValueList.valueId",valueIds);
//        bool.filter(term2);

        // 查询条件
        if(StringUtils.isNotBlank(skuLsParam.getKeyword())){
            MatchQueryBuilder match1 = new MatchQueryBuilder("skuName", skuLsParam.getKeyword());
            bool.must(match1);
        }
        // 放入query条件
        searchTools.query(bool);

        // 排序，分页from和size，高亮
        searchTools.sort("hotScore");
        searchTools.from(0);
        searchTools.size(50);
        HighlightBuilder h = new HighlightBuilder();
        h.preTags("<span style='color:red;font-weight:bolder'>");
        h.field("skuName");
        h.postTags("</span>");
        searchTools.highlight(h);

        // 封装聚合条件
        TermsBuilder groupby_attr = AggregationBuilders.terms("aggs_valueId").field("skuAttrValueList.valueId");
        searchTools.aggregation(groupby_attr);
        // 打印dsl
        String query = searchTools.toString();
        System.out.println(query);

        // 执行查询语句
        Search search = new Search.Builder(query).addIndex("gmall").addType("SkuLsInfo").build();

        // 解析返回结果
        SearchResult execute = null;
        List<SkuLsInfo> skuLsInfoList = new ArrayList<SkuLsInfo>();
        try {
            execute = jestClient.execute(search);
            List<SearchResult.Hit<SkuLsInfo, Void>> hits = execute.getHits(SkuLsInfo.class);

            for (SearchResult.Hit<SkuLsInfo, Void> hit : hits) {
                SkuLsInfo sku = hit.source;
                Map<String, List<String>> highlight = hit.highlight;
                if(highlight!=null&&!highlight.isEmpty()){
                    String highlightSkuName =   highlight.get("skuName").get(0);
                    sku.setSkuName(highlightSkuName);
                }
                skuLsInfoList.add(sku);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 解析聚合结果
        MetricAggregation aggregations = execute.getAggregations();
        TermsAggregation groupby_valueId = aggregations.getTermsAggregation("aggs_valueId");
        List<TermsAggregation.Entry> buckets = groupby_valueId.getBuckets();
        // 将buckets中的结果封装到属性id集合中
        List<String> valueIdList=new ArrayList<>(buckets.size());
        for (TermsAggregation.Entry bucket : buckets) {
            String valueId = bucket.getKey();
            valueIdList.add(valueId);
        }

        return skuLsInfoList;
    }
}
