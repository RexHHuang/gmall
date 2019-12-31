package com.atguigu.gmall.item.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.PmsProductSaleAttr;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.bean.PmsSkuSaleAttrValue;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.service.SpuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ItemController {

    @Reference
    private SkuService skuService;

    @Reference
    private SpuService spuService;

    @RequestMapping("{skuId}.html")
    public String item(@PathVariable String skuId, ModelMap modelMap, HttpServletRequest request) {
        String ip = request.getRemoteAddr();

        PmsSkuInfo skuInfo = skuService.getSkuByIdAndCache(skuId, ip);
        modelMap.put("skuInfo", skuInfo);

        // 同一个spu销售属性列表结合
        List<PmsProductSaleAttr> spuSaleAttrList = spuService.spuSaleAttrListCheckBySku(skuInfo.getSpuId(), skuId);
        modelMap.put("spuSaleAttrListCheckBySku", spuSaleAttrList);

        /*
            通过当前的sku查询同意spu下的所有sku表，包括当前sku自己
            通过类似spu下的销售属性组合 240|239| 这样的销售属性值id来看数据库中是否有对应的
            某一个具体sku如果没有，则说明没有这种组合，

            此方法仍然要每次都去查数据库，也不是高效的做法
         */
        Map<String, String> map = new HashMap<>();
        List<PmsSkuInfo> skuInfos = skuService.getAllSkuBySpu(skuInfo.getSpuId());
        for (PmsSkuInfo pmsSkuInfo : skuInfos) {
            String value = pmsSkuInfo.getId();
            List<PmsSkuSaleAttrValue> skuSaleAttrValueList = pmsSkuInfo.getSkuSaleAttrValueList();
            String key = "";
            for (PmsSkuSaleAttrValue pmsSkuSaleAttrValue : skuSaleAttrValueList) {
                key += pmsSkuSaleAttrValue.getSaleAttrValueId() + "|";
            }
            map.put(key, value);
        }

        // 将此map放在页面上，等待使用
        String existSkuJsonStr = JSON.toJSONString(map);
        modelMap.put("existSkuJsonStr", existSkuJsonStr);

        return "item";
    }

    @RequestMapping("/index")
    public String index(ModelMap map) {
        map.put("hello", "hello, thymeleaf!");
        return "index";
    }

}
