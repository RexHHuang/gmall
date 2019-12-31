package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.PmsProductImage;
import com.atguigu.gmall.bean.PmsProductInfo;
import com.atguigu.gmall.bean.PmsProductSaleAttr;
import com.atguigu.gmall.bean.PmsProductSaleAttrValue;
import com.atguigu.gmall.manage.mapper.PmsProductImageMapper;
import com.atguigu.gmall.manage.mapper.PmsProductInfoMapper;
import com.atguigu.gmall.manage.mapper.PmsProductSaleAttrMapper;
import com.atguigu.gmall.manage.mapper.PmsProductSaleAttrValueMapper;
import com.atguigu.gmall.service.SpuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

@Service
public class SpuServiceImpl implements SpuService {

    @Autowired
    PmsProductInfoMapper productInfoMapper;

    @Autowired
    PmsProductImageMapper productImageMapper;

    @Autowired
    PmsProductSaleAttrMapper productSaleAttrMapper;

    @Autowired
    PmsProductSaleAttrValueMapper productSaleAttrValueMapper;

    @Override
    public List<PmsProductInfo> supList(String catalog3Id) {
        PmsProductInfo record = new PmsProductInfo();
        record.setCatalog3Id(catalog3Id);
        return productInfoMapper.select(record);
    }

    @Transactional
    @Override
    public void saveSpuInfo(PmsProductInfo pmsProductInfo) {
        // 先保存产品的spu基本信息
        productInfoMapper.insertSelective(pmsProductInfo);
        String productId = pmsProductInfo.getId();
        System.out.println("=========产品id：" + productId + "============");

        // 保存图片url信息
        List<PmsProductImage> spuImageList = pmsProductInfo.getSpuImageList();
        for (PmsProductImage pmsProductImage : spuImageList) {
            pmsProductImage.setProductId(productId);
            productImageMapper.insertSelective(pmsProductImage);
        }

        // 保存销售属性
        List<PmsProductSaleAttr> spuSaleAttrList = pmsProductInfo.getSpuSaleAttrList();
        for (PmsProductSaleAttr pmsProductSaleAttr : spuSaleAttrList) {
            pmsProductSaleAttr.setProductId(productId);
            productSaleAttrMapper.insertSelective(pmsProductSaleAttr);
            // 保存销售属性的值
            List<PmsProductSaleAttrValue> spuSaleAttrValueList = pmsProductSaleAttr.getSpuSaleAttrValueList();
            for (PmsProductSaleAttrValue pmsProductSaleAttrValue : spuSaleAttrValueList) {
                pmsProductSaleAttrValue.setProductId(productId);
                productSaleAttrValueMapper.insertSelective(pmsProductSaleAttrValue);
            }
        }
    }

    @Override
    public List<PmsProductSaleAttr> spuSaleAttrList(String spuId) {
        PmsProductSaleAttr record = new PmsProductSaleAttr();
        record.setProductId(spuId);
        List<PmsProductSaleAttr> productSaleAttrs = productSaleAttrMapper.select(record);
        for (PmsProductSaleAttr productSaleAttr : productSaleAttrs) {
            Example example = new Example(PmsProductSaleAttrValue.class);
            example.createCriteria()
                    .andEqualTo("productId", spuId)
                    .andEqualTo("saleAttrId", productSaleAttr.getSaleAttrId());

            List<PmsProductSaleAttrValue> productSaleAttrValues = productSaleAttrValueMapper.selectByExample(example);
            productSaleAttr.setSpuSaleAttrValueList(productSaleAttrValues);
        }
        return productSaleAttrs;
    }

    @Override
    public List<PmsProductImage> spuImageList(String spuId) {
        PmsProductImage record = new PmsProductImage();
        record.setProductId(spuId);
        return productImageMapper.select(record);
    }

    /**
     * 前台页面，根据sku，查出整个spu下对应的销售属性，以及属性值
     * @param spuId
     * @return
     */
    @Override
    public List<PmsProductSaleAttr> spuSaleAttrListCheckBySku(String spuId, String skuId) {
//        PmsProductSaleAttr spuSaleAttr = new PmsProductSaleAttr();
//        spuSaleAttr.setProductId(spuId);
//        List<PmsProductSaleAttr> productSaleAttrs = productSaleAttrMapper.select(spuSaleAttr);
//
//        // 再查出spu下对应产品属性的属性值
//        for (PmsProductSaleAttr productSaleAttr : productSaleAttrs) {
//            PmsProductSaleAttrValue record = new PmsProductSaleAttrValue();
//            record.setProductId(spuId);
//            record.setSaleAttrId(productSaleAttr.getSaleAttrId());
//            List<PmsProductSaleAttrValue> spuSaleAttrValues = productSaleAttrValueMapper.select(record);
//            productSaleAttr.setSpuSaleAttrValueList(spuSaleAttrValues);
//        }
        List<PmsProductSaleAttr> productSaleAttrs = productSaleAttrMapper.selectSpuSaleAttrListCheckBySku(spuId, skuId);
        return productSaleAttrs;
    }
}
