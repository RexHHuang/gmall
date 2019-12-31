package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.SearchParam;
import com.atguigu.gmall.bean.SearchSkuInfo;

import java.util.List;

public interface SearchService {

    List<SearchSkuInfo> list(SearchParam searchParam);
}
