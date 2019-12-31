package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.PmsBaseAttrInfo;
import com.atguigu.gmall.bean.PmsBaseAttrValue;
import com.atguigu.gmall.bean.PmsBaseSaleAttr;
import com.atguigu.gmall.manage.mapper.PmsBaseAttrInfoMapper;
import com.atguigu.gmall.manage.mapper.PmsBaseAttrValueMapper;
import com.atguigu.gmall.manage.mapper.PmsBaseSaleAttrMapper;
import com.atguigu.gmall.service.AttrService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.util.List;
import java.util.Set;

@Service
public class AttrServiceImpl implements AttrService {

    @Autowired
    PmsBaseAttrInfoMapper attrInfoMapper;

    @Autowired
    PmsBaseAttrValueMapper attrValueMapper;

    @Autowired
    PmsBaseSaleAttrMapper saleAttrMapper;

    @Override
    public List<PmsBaseAttrInfo> attrInfoList(String catalog3Id) {
        PmsBaseAttrInfo record = new PmsBaseAttrInfo();
        record.setCatalog3Id(catalog3Id);
        List<PmsBaseAttrInfo> baseAttrInfos = attrInfoMapper.select(record);
        for (PmsBaseAttrInfo baseAttrInfo : baseAttrInfos) {
            // 查出此属性的所有value
            PmsBaseAttrValue baseAttrValue = new PmsBaseAttrValue();
            baseAttrValue.setAttrId(baseAttrInfo.getId());
            List<PmsBaseAttrValue> baseAttrValues = attrValueMapper.select(baseAttrValue);

            baseAttrInfo.setAttrValueList(baseAttrValues);
        }
        return baseAttrInfos;
    }


    /**
     * 保存平台属性
     * 保存和修改写在同一个方法中
     */
    @Override
    @Transactional
    public String saveAttrInfo(PmsBaseAttrInfo pmsBaseAttrInfo) {
        String id = pmsBaseAttrInfo.getId();
        if (StringUtils.isBlank(id)) {
            // id 为空，则是保存
            // 保存属性
            attrInfoMapper.insertSelective(pmsBaseAttrInfo);
            // 保存属性值
            List<PmsBaseAttrValue> attrValueList = pmsBaseAttrInfo.getAttrValueList();
            for (PmsBaseAttrValue attrValue : attrValueList) {
                attrValue.setAttrId( pmsBaseAttrInfo.getId() );
                attrValueMapper.insertSelective(attrValue);
            }
        } else {
            // id 不为空，则是修改
            // 修改属性
            Example example = new Example(PmsBaseAttrInfo.class);
            example.createCriteria().andEqualTo("id", pmsBaseAttrInfo.getId());
            attrInfoMapper.updateByExampleSelective(pmsBaseAttrInfo, example);

            // 先删除原来表中存在的属性值
            PmsBaseAttrValue attrValueDel = new PmsBaseAttrValue();
            attrValueDel.setAttrId(pmsBaseAttrInfo.getId());
            attrValueMapper.delete(attrValueDel);
            // 修改属性值
            List<PmsBaseAttrValue> attrValueList = pmsBaseAttrInfo.getAttrValueList();
            for (PmsBaseAttrValue attrValue : attrValueList) {
                attrValue.setAttrId(pmsBaseAttrInfo.getId());
                // 设置更新的条件
                attrValueMapper.insertSelective(attrValue);
            }
        }

        return "success";
    }

    /**
     * 根据属性id获取属性值
     */
    @Override
    public List<PmsBaseAttrValue> getAttrValueList(String attrId) {
        PmsBaseAttrValue record = new PmsBaseAttrValue();
        record.setAttrId(attrId);

        return attrValueMapper.select(record);
    }

    /**
     * 添加spu信息时，获取基本的销售属性
     * @return
     */
    @Override
    public List<PmsBaseSaleAttr> baseSaleAttrList() {
        return saleAttrMapper.selectAll();
    }

    /**
     * 搜索功能：
     * 根据属性值的id查询属性的各种信息
     */
    @Override
    public List<PmsBaseAttrInfo> getAttrInfoListByValueId(Set<String> valueIdSet) {
        String valueIdStr = StringUtils.join(valueIdSet, ",");
        System.out.println(valueIdStr);
        return attrInfoMapper.selectAttrInfoListByValueId(valueIdStr);
    }


}
