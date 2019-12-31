package com.atguigu.gmall.search.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.annotations.AuthRequired;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.service.AttrService;
import com.atguigu.gmall.service.SearchService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.*;

@Controller
public class SearchController {

    @Reference
    private SearchService searchService;

    @Reference
    private AttrService attrService;

    @RequestMapping("/index")
    @AuthRequired(loginMust = false)
    public String index() {
        return "index";
    }

    @RequestMapping("/list.html")
    public String list(SearchParam searchParam, ModelMap modelMap) {
        // 调用搜索服务，返回搜索结果
        List<SearchSkuInfo> searchSkuInfos = searchService.list(searchParam);
        modelMap.put("skuLsInfoList", searchSkuInfos);

        // 抽取检索结果所包含的平台属性集合
        String[] valueIds = searchParam.getValueId();

        List<String> valueIdList = null;
        if (valueIds != null) {
            valueIdList = Arrays.asList(valueIds);
        }
        Set<String> valueIdSet = new HashSet<>();
        for (SearchSkuInfo searchSkuInfo : searchSkuInfos) {
            List<PmsSkuAttrValue> skuAttrValueList = searchSkuInfo.getSkuAttrValueList();
            for (PmsSkuAttrValue pmsSkuAttrValue : skuAttrValueList) {
                String valueId = pmsSkuAttrValue.getValueId();
                // 当这些valueId不是筛选条件的valueId，就把它添加到set中去，如果是筛选条件的属性id就不填进去进行页面的展示
                if (valueIdList == null || !valueIdList.contains(valueId)) {
                    // 对平台属性集合进一步处理，去掉当前条件中valueId所在的属性组
                    valueIdSet.add(valueId);
                }
            }
        }

        // 根据valueId将属性列表查询出来
        List<PmsBaseAttrInfo> pmsBaseAttrInfos = attrService.getAttrInfoListByValueId(valueIdSet);
        modelMap.put("attrList", pmsBaseAttrInfos);

        String urlParam = getUrlParam(searchParam);
        modelMap.put("urlParam", urlParam);

        // 如果有keyword，就在筛选条件处展示一下
        String keyword = searchParam.getKeyword();
        if (StringUtils.isNotBlank(keyword)) {
            modelMap.put("keyword", keyword);
        }

        // 创建面包屑
        List<SearchCrumb> searchCrumbs = new ArrayList<>();
        if (valueIds != null) {
            // 此处构建valueId的set再去查数据库，只是为了得到valueName，比较耗费性能
            Set<String> set = new HashSet<>();
            for (String valueId : valueIds) {
                set.add(valueId);
            }
            List<PmsBaseAttrInfo> attrInfoListByValueId = attrService.getAttrInfoListByValueId(set);
            // 如果valueIds参数不为空，则说明当前请求中包含属性的参数，每一个属性参数都会生成一个面包屑
            for (String valueId : valueIds) {
                SearchCrumb searchCrumb = new SearchCrumb();
                // 设定面包屑的基本属性的值
                for (PmsBaseAttrInfo pmsBaseAttrInfo : attrInfoListByValueId) {
                    List<PmsBaseAttrValue> attrValueList = pmsBaseAttrInfo.getAttrValueList();
                    for (PmsBaseAttrValue attrValue : attrValueList) {
                        if (attrValue.getId().equals(valueId)) {
                            searchCrumb.setValueName(attrValue.getValueName());
                        }
                    }
                }
                // 生成面包屑的参数
                searchCrumb.setUrlParam(getUrlParamForCrumb(searchParam, valueId));

                searchCrumbs.add(searchCrumb);
            }
        }
        modelMap.put("attrValueSelectedList", searchCrumbs);

        return "list";
    }

    private String getUrlParam(SearchParam searchParam) {
        // keyword和catalog3Id肯定是至少有一个
        String keyword = searchParam.getKeyword();
        String catalog3Id = searchParam.getCatalog3Id();
        String[] valueIds = searchParam.getValueId();

        String urlParam = "";

        if (StringUtils.isNotBlank(keyword)) {
            urlParam = urlParam + "&keyword=" + keyword;
        }

        if (StringUtils.isNotBlank(catalog3Id)) {
            urlParam = urlParam + "&catalog3Id=" + catalog3Id;
        }

        if (valueIds != null) {
            for (String valueId : valueIds) {
                urlParam = urlParam + "&valueId=" + valueId;
            }
        }

        if (urlParam.startsWith("&")) {
            urlParam = urlParam.substring(1);
        }
        return urlParam;
    }

    private String getUrlParamForCrumb(SearchParam searchParam, String valueId) {
        // keyword和catalog3Id肯定是至少有一个
        String keyword = searchParam.getKeyword();
        String catalog3Id = searchParam.getCatalog3Id();
        String[] valueIds = searchParam.getValueId();

        String urlParam = "";

        if (StringUtils.isNotBlank(keyword)) {
            urlParam = urlParam + "&keyword=" + keyword;
        }

        if (StringUtils.isNotBlank(catalog3Id)) {
            urlParam = urlParam + "&catalog3Id=" + catalog3Id;
        }

        if (valueIds != null) {
            for (String id : valueIds) {
                if (!id.equals(valueId)) {
                    urlParam += "&valueId=" + id;
                }
            }
        }

        if (urlParam.startsWith("&")) {
            urlParam = urlParam.substring(1);
        }
        return urlParam;
    }

}
