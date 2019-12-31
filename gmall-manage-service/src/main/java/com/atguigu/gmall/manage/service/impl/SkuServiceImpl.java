package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.PmsSkuAttrValue;
import com.atguigu.gmall.bean.PmsSkuImage;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.bean.PmsSkuSaleAttrValue;
import com.atguigu.gmall.manage.mapper.PmsSkuAttrValueMapper;
import com.atguigu.gmall.manage.mapper.PmsSkuImageMpper;
import com.atguigu.gmall.manage.mapper.PmsSkuInfoMapper;
import com.atguigu.gmall.manage.mapper.PmsSkuSaleAttrValueMapper;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class SkuServiceImpl implements SkuService {

    @Autowired
    private PmsSkuInfoMapper skuInfoMapper;

    @Autowired
    private PmsSkuImageMpper skuImageMpper;

    @Autowired
    private PmsSkuAttrValueMapper skuAttrValueMapper;

    @Autowired
    private PmsSkuSaleAttrValueMapper skuSaleAttrValueMapper;

    @Autowired
    RedisUtil redisUtil;

    @Override
    @Transactional
    public String saveSkuInfo(PmsSkuInfo pmsSkuInfo) {
        // 先保存sku的基本信息
        skuInfoMapper.insertSelective(pmsSkuInfo);

        // 拿出sku表的id
        String skuId = pmsSkuInfo.getId();
        // 拿出sku图片的list 保存sku图片表
        List<PmsSkuImage> skuImageList = pmsSkuInfo.getSkuImageList();
        for (PmsSkuImage pmsSkuImage : skuImageList) {
            pmsSkuImage.setSkuId(skuId);
            skuImageMpper.insertSelective(pmsSkuImage);
        }

        // 保存sku的平台属性和平台属性值
        List<PmsSkuAttrValue> skuAttrValueList = pmsSkuInfo.getSkuAttrValueList();
        for (PmsSkuAttrValue pmsSkuAttrValue : skuAttrValueList) {
            pmsSkuAttrValue.setSkuId(skuId);
            skuAttrValueMapper.insertSelective(pmsSkuAttrValue);
        }

        // 保存sku的销售属性值
        List<PmsSkuSaleAttrValue> skuSaleAttrValueList = pmsSkuInfo.getSkuSaleAttrValueList();
        for (PmsSkuSaleAttrValue pmsSkuSaleAttrValue : skuSaleAttrValueList) {
            pmsSkuSaleAttrValue.setSkuId(skuId);
            skuSaleAttrValueMapper.insertSelective(pmsSkuSaleAttrValue);
        }

        return "success";

    }

    /**
     * 此方法查询sku没有redis缓存
     * @param skuId
     * @return
     */
    @Override

    public PmsSkuInfo getSkuById(String skuId) {
        PmsSkuInfo skuInfo = skuInfoMapper.selectByPrimaryKey(skuId);
        // 查询图片列表
        if (skuInfo != null) {
            PmsSkuImage record = new PmsSkuImage();
            record.setSkuId(skuId);
            List<PmsSkuImage> skuImages = skuImageMpper.select(record);
            // 防止这里出现空指针异常
            skuInfo.setSkuImageList(skuImages);
        }
        return skuInfo;
    }

    /**
     * 带有redis缓存的查询sku
     */
    @Override
    public PmsSkuInfo getSkuByIdAndCache(String skuId, String ip) {
        PmsSkuInfo skuInfo = null;
        // 连接缓存
        Jedis jedis = redisUtil.getJedis();
        // 查询缓存
        String skuKey = "sku:" + skuId + ":info";

//        System.out.println("ip为：" + ip + "的同学，线程为：" + Thread.currentThread().getSkuName() + "开始从缓存中拿数据");

        String skuJson = jedis.get(skuKey);
        if (StringUtils.isNotBlank(skuJson)){
            skuInfo = JSON.parseObject(skuJson, PmsSkuInfo.class);
        } else {

//            System.out.println("ip为：" + ip + "的同学，线程为：" + Thread.currentThread().getSkuName() + "，发现缓存中没有数据，开始拿锁");
            // 如果缓存中没有，查询 mysql
            // 使用此token防止此线程因为某些原因获得数据比较慢，而锁已经过期去删除别人的锁
            String token = UUID.randomUUID().toString();
            // 设置分布式锁，防止缓存击穿， nx 当key不存在时才set
            String str = jedis.set("sku:" + skuId + ":lock", token, "nx", "px", 20 * 1000);
            if (StringUtils.isNotBlank(str) && str.equals("OK")){

//                System.out.println("ip为：" + ip + "的同学，线程为：" + Thread.currentThread().getSkuName() + "拿到锁，开始从mysql中拿数据");
                // 设置成功，有权在10秒的过期时间内访问数据库
                skuInfo = getSkuById(skuId);

//                try {
//                    Thread.sleep(10 * 1000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }

                if (skuInfo != null) {
                    // mysql 查询结果存入 redis
                    jedis.set("sku:" + skuId + ":info", JSON.toJSONString(skuInfo));
                } else {
                    // 数据库中不存在该sku，为了防止缓存穿透，利用不存在key，不断访问数据库的现象，
                    // 我们将null值或者空字符串也设置给redis
                    // 60 * 3 为此空字符串的过期时间
                    jedis.setex("sku:" + skuId + ":info", 60 * 3, JSON.toJSONString(""));
                }
//                System.out.println("ip为：" + ip + "的同学，线程为：" + Thread.currentThread().getSkuName() + "，拿到数据释放锁");
                // 在访问mysql后，将mysql的分布式锁释放
                String lockToken = jedis.get("sku:" + skuId + ":lock");
                // 如果在高并发情况下，在判断的一瞬间，锁过期了怎么办？用lua脚本
//                String script = "if redis.call('get', keys[1]) == argv[1] then return redis.call('del', keys[1]) else return 0 end";
//                jedis.eval(script, Collections.singletonList("sku:" + skuId + ":lock"), Collections.singletonList(token));
                if (StringUtils.isNotBlank(lockToken) && lockToken.equals(token)) {

                    jedis.del("sku:" + skuId + ":lock");
                }
            }
            else {
//                System.out.println("ip为：" + ip + "的同学，线程为：" + Thread.currentThread().getSkuName() + "，没拿到数据开始自旋");
                // 如果设置失败，自旋（该线程在睡眠几秒后，重新尝试去获取数据）
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return getSkuByIdAndCache(skuId, ip);
            }
        }
        jedis.close();
        return skuInfo;
    }

    @Override
    public List<PmsSkuInfo> getAllSkuBySpu(String spuId) {
        return skuInfoMapper.selectALlSkuBySpu(spuId);
    }

    @Override
    public List<PmsSkuInfo> getAllSku(String catalog3Id) {
        List<PmsSkuInfo> pmsSkuInfos = skuInfoMapper.selectAll();

        for (PmsSkuInfo pmsSkuInfo : pmsSkuInfos) {
            String skuId = pmsSkuInfo.getId();

            PmsSkuAttrValue pmsSkuAttrValue = new PmsSkuAttrValue();
            pmsSkuAttrValue.setSkuId(skuId);
            List<PmsSkuAttrValue> select = skuAttrValueMapper.select(pmsSkuAttrValue);
            pmsSkuInfo.setSkuAttrValueList(select);
        }
        return pmsSkuInfos;
    }

    @Override
    public boolean checkPrice(String productSkuId, BigDecimal price) {

        boolean b = false;
        PmsSkuInfo skuInfo = new PmsSkuInfo();
        skuInfo.setId(productSkuId);
        PmsSkuInfo skuInfo1 = skuInfoMapper.selectOne(skuInfo);
        BigDecimal priceFromDb = skuInfo1.getPrice();
        if (price.compareTo(priceFromDb) == 0) {
            b = true;
        }
        return b;
    }
}
