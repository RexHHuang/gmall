package com.atguigu.gmall.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.OmsCartItem;
import com.atguigu.gmall.cart.mapper.CartMapper;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
public class CartServiceImpl implements CartService {

    @Autowired
    CartMapper cartMapper;

    @Autowired
    RedisUtil redisUtil;

    @Override
    public OmsCartItem getCartItemByUser(String memberId, String skuId) {

        OmsCartItem cartItem = new OmsCartItem();
        cartItem.setMemberId(memberId);
        cartItem.setProductSkuId(skuId);
        OmsCartItem itemFromDb = cartMapper.selectOne(cartItem);
        return itemFromDb;
    }

    @Override
    public void addToCart(OmsCartItem cartItem) {

        if (StringUtils.isNotBlank(cartItem.getMemberId())) {
            cartMapper.insert(cartItem);
        }
    }

    @Override
    public void updateCart(OmsCartItem itemInDb) {
        Example e = new Example(OmsCartItem.class);
        e.createCriteria().andEqualTo("id", itemInDb.getId());

        cartMapper.updateByExampleSelective(itemInDb, e);
    }

    @Override
    public void synchronizeCartCache(String memberId) {
        OmsCartItem cartItem = new OmsCartItem();
        cartItem.setMemberId(memberId);
        List<OmsCartItem> cartItems = cartMapper.select(cartItem);

        Map<String , String> map = new HashMap<>();
        for (OmsCartItem item : cartItems) {
            item.setTotalPrice(item.getPrice().multiply(item.getQuantity()));
            map.put(item.getProductSkuId(), JSON.toJSONString(item));
        }
        // 同步到redis缓存中
        final Jedis jedis = redisUtil.getJedis();
        jedis.del("user:" + memberId + ":cart");
        jedis.hmset("user:" + memberId + ":cart", map);

        jedis.close();

    }

    @Override
    public List<OmsCartItem> cartList(String memberId) {
        Jedis jedis = null;
        List<OmsCartItem> cartItems = new ArrayList<>();
        try {
            jedis = redisUtil.getJedis();
            List<String> hvals = jedis.hvals("user:" + memberId + ":cart");
            for (String hval : hvals) {
                OmsCartItem cartItem = JSON.parseObject(hval, OmsCartItem.class);
//                cartItem.setTotalPrice(cartItem.getPrice().multiply(cartItem.getQuantity()));
                cartItems.add(cartItem);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            jedis.close();
        }
        return cartItems;
    }

    @Override
    public void checkCart(String isChecked, String skuId, String memberId) {
        Example e = new Example(OmsCartItem.class);
        e.createCriteria().andEqualTo("memberId", memberId).andEqualTo("productSkuId", skuId);
        OmsCartItem cartItem = new OmsCartItem();
        cartItem.setIsChecked(isChecked);
        cartMapper.updateByExampleSelective(cartItem, e);

        // 缓存同步
        synchronizeCartCache(memberId);
    }
}
