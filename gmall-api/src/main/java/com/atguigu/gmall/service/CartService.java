package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.OmsCartItem;

import java.util.List;

public interface CartService {

    OmsCartItem getCartItemByUser(String memberId, String skuId);

    void addToCart(OmsCartItem cartItem);

    void updateCart(OmsCartItem itemInDb);

    void synchronizeCartCache(String memberId);

    List<OmsCartItem> cartList(String memberId);

    void checkCart(String isChecked, String skuId, String memeberId);
}
