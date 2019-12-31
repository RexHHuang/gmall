package com.atguigu.gmall.search;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.bean.SearchSkuInfo;
import com.atguigu.gmall.service.SkuService;
import io.searchbox.client.JestClient;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallSearchServiceApplicationTest {

    @Reference
    SkuService skuService;  // 查询 mysql

    @Autowired
    JestClient jestClient;

    @Test
    public void contextLoads() throws IOException {
        // 用api执行复制es查询操作
        /*
        GET gmall0105/pmsSkuInfo/_search
        {
            "query": {
                "bool": {
                    "filter": [{"term": {"skuAttrValueList.valueId": "39"}}, {"term": { "skuAttrValueList.valueId": "43"}}],
                    "must": [{ "match": {"skuName": "小米"} }]
                 }
            }
        }
         */
        // jest 的dsl工具
        // bool
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        // filter
        TermQueryBuilder termQueryBuilder1 = new TermQueryBuilder("skuAttrValueList.valueId", "39");
        TermQueryBuilder termQueryBuilder2 = new TermQueryBuilder("skuAttrValueList.valueId", "43");
        boolQueryBuilder.filter(termQueryBuilder1);
        boolQueryBuilder.filter(termQueryBuilder2);
        // must
        MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("skuName", "小米");
        boolQueryBuilder.must(matchQueryBuilder);
        // query(bool-filter-must), from, size, highlight
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(boolQueryBuilder);

        String dslStr = searchSourceBuilder.toString();
        Search search = new Search.Builder(dslStr).addIndex("gmall0105")
                .addType("pmsSkuInfo")
                .build();

        List<SearchSkuInfo> searchSkuInfos = new ArrayList<>();

        SearchResult result = jestClient.execute(search);
        List<SearchResult.Hit<SearchSkuInfo, Void>> hits = result.getHits(SearchSkuInfo.class);
        for (SearchResult.Hit<SearchSkuInfo, Void> hit : hits) {
            SearchSkuInfo source = hit.source;

            searchSkuInfos.add(source);
        }
        System.out.println(searchSkuInfos.size());
    }

    @Test
    public void mysql2EsConvert() throws IOException {
        // 查询mysq数据
        List<PmsSkuInfo> pmsSkuInfos = skuService.getAllSku("61");
        // 转化为es的数据结构
        List<SearchSkuInfo> searchSkuInfos = new ArrayList<>();

        for (PmsSkuInfo skuInfo : pmsSkuInfos) {
            SearchSkuInfo searchSkuInfo = new SearchSkuInfo();
            BeanUtils.copyProperties(skuInfo, searchSkuInfo);
            searchSkuInfos.add(searchSkuInfo);
        }

        // 导入es
        for (PmsSkuInfo skuInfo : pmsSkuInfos) {
            Index put = new Index.Builder(skuInfo).index("gmall0105").type("pmsSkuInfo").id(skuInfo.getId()).build();
            jestClient.execute(put);
        }
    }
}
