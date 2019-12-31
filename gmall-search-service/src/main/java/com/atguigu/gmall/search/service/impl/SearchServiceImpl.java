package com.atguigu.gmall.search.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.PmsSkuAttrValue;
import com.atguigu.gmall.bean.SearchParam;
import com.atguigu.gmall.bean.SearchSkuInfo;
import com.atguigu.gmall.service.SearchService;
import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SearchServiceImpl implements SearchService {

    @Autowired
    JestClient jestClient;

    @Override
    public List<SearchSkuInfo> list(SearchParam searchParam) {

        String dslStr = getSearchDsl(searchParam);
        System.out.println(dslStr);

        Search search = new Search.Builder(dslStr).addIndex("gmall0105")
                .addType("pmsSkuInfo")
                .build();

        List<SearchSkuInfo> searchSkuInfos = new ArrayList<>();

        SearchResult result = null;
        try {
            result = jestClient.execute(search);
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<SearchResult.Hit<SearchSkuInfo, Void>> hits = result.getHits(SearchSkuInfo.class);
        for (SearchResult.Hit<SearchSkuInfo, Void> hit : hits) {
            SearchSkuInfo source = hit.source;

            Map<String, List<String>> highlight = hit.highlight;
            // 此处可能出现空指针异常，如果搜索的商品不是从搜索框键入的话
            if (highlight != null && highlight.get("skuName") != null && highlight.get("skuName").size() != 0) {
                String skuName = highlight.get("skuName").get(0);
                source.setSkuName(skuName);
            }
            searchSkuInfos.add(source);
        }
        return searchSkuInfos;
    }

    private String getSearchDsl(SearchParam searchParam) {
        // 先拿到用户传递过来要进行搜索的参数
        String[] valueIds = searchParam.getValueId();
        String keyword = searchParam.getKeyword();
        String catalog3Id = searchParam.getCatalog3Id();

        // jest 的dsl工具
        // bool
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        // filter
        if (StringUtils.isNotBlank(catalog3Id)) {
            TermQueryBuilder termQueryBuilder = new TermQueryBuilder("catalog3Id", catalog3Id);
            boolQueryBuilder.filter(termQueryBuilder);
        }
        if (valueIds != null) {
            for (String valueId : valueIds) {
                TermQueryBuilder termQueryBuilder = new TermQueryBuilder("skuAttrValueList.valueId", valueId);
                boolQueryBuilder.filter(termQueryBuilder);
            }
        }

        // must
        if (StringUtils.isNotBlank(keyword)) {
            MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("skuName", keyword);
            boolQueryBuilder.must(matchQueryBuilder);
        }
        // query(bool-filter-must), from, size, highlight
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(boolQueryBuilder);

        searchSourceBuilder.from(0);
        searchSourceBuilder.size(20);
        // sort
        searchSourceBuilder.sort("id", SortOrder.DESC);

        // highlight
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.preTags("<span style='color:red;'>");
        highlightBuilder.field("skuName");
        highlightBuilder.postTags("</span>");
        searchSourceBuilder.highlight(highlightBuilder);

        // aggs 利用es进行聚合查出有哪些属性，也可以在controller中利用java代码聚合
//        TermsBuilder groupby_attr = AggregationBuilders.terms("groupby_attr").field("skuAttrValueList.valueId");
//        searchSourceBuilder.aggregation(groupby_attr);

        return  searchSourceBuilder.toString();
    }
}
