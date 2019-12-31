package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.PmsBaseCatalog1;
import com.atguigu.gmall.bean.PmsBaseCatalog2;
import com.atguigu.gmall.bean.PmsBaseCatalog3;
import com.atguigu.gmall.manage.mapper.PmsBaseCatalog1Mapper;
import com.atguigu.gmall.manage.mapper.PmsBaseCatalog2Mapper;
import com.atguigu.gmall.manage.mapper.PmsBaseCatalog3Mapper;
import com.atguigu.gmall.service.CatalogService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Service
public class CatalogServiceImpl implements CatalogService {

    @Autowired
    PmsBaseCatalog1Mapper baseCatalog1Mapper;

    @Autowired
    PmsBaseCatalog2Mapper baseCatalog2Mapper;

    @Autowired
    PmsBaseCatalog3Mapper baseCatalog3Mapper;

    @Override
    public List<PmsBaseCatalog1> getCatalog1() {
        return baseCatalog1Mapper.selectAll();
    }

    @Override
    public List<PmsBaseCatalog2> getCatalog2(String catalog1Id) {
        PmsBaseCatalog2 record = new PmsBaseCatalog2();
        record.setCatalog1Id(catalog1Id);
        return baseCatalog2Mapper.select(record);
    }

    @Override
    public List<PmsBaseCatalog3> getCatalog3(String catalog2Id) {
        PmsBaseCatalog3 record = new PmsBaseCatalog3();
        record.setCatalog2Id(catalog2Id);
        return baseCatalog3Mapper.select(record);
    }
}
