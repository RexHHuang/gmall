package com.atguigu.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.annotations.AuthRequired;
import com.atguigu.gmall.bean.OmsCartItem;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.util.CookieUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Controller
public class CartController {

    @Reference
    SkuService skuService;

    @Reference
    CartService cartService;


    // ajax请求，修改商品的选中状态，利用的一个内嵌的页面 cartListInner
    @RequestMapping("/checkCart")
    @AuthRequired(loginMust = false)
    public String checkCart(String isChecked, String skuId, ModelMap modelMap, HttpServletRequest request) {

        String memberId = (String)request.getAttribute("memberId");
        String nickname = (String)request.getAttribute("nickName");
        // 调用服务，修改状态
        cartService.checkCart(isChecked, skuId, memberId);
        // 将最新的数据从缓存中查出，渲染给内嵌页
        List<OmsCartItem> cartItems = cartService.cartList(memberId);
        modelMap.put("cartList", cartItems);
        // 被勾选的商品的总额
        BigDecimal amount = getAmount(cartItems);
        modelMap.put("amount", amount);
        return "cartListInner";
    }

    // 用户去购物车时展示购物车数据
    @RequestMapping("/cartList")
    @AuthRequired(loginMust = false)
    public String cartList(HttpServletRequest request, ModelMap modelMap) {

        List<OmsCartItem> cartItems = new ArrayList<>();
        String memberId = (String)request.getAttribute("memberId");
        String nickname = (String)request.getAttribute("nickName");
        if (StringUtils.isNotBlank(memberId)) {
            // 已经登录查询DB(基于缓存的查询)
            cartItems = cartService.cartList(memberId);
        } else {
            // 没有登录查询Cookie
            String cartListCookie = CookieUtil.getCookieValue(request, "cartListCookie", true);
            if (StringUtils.isNotBlank(cartListCookie)) {
                cartItems = JSON.parseArray(cartListCookie, OmsCartItem.class);
            }
        }
//        for (OmsCartItem cartItem : cartItems) {
//            cartItem.setTotalPrice(cartItem.getPrice().multiply(cartItem.getQuantity()));
//
//        }
        modelMap.put("cartList", cartItems);
        // 被勾选的商品的总额

        BigDecimal amount = getAmount(cartItems);
        modelMap.put("amount", amount);
        return "cartList";
    }

    private BigDecimal getAmount(List<OmsCartItem> cartItems) {
        BigDecimal amount = new BigDecimal("0");
        for (OmsCartItem cartItem : cartItems) {
            if (cartItem.getIsChecked().equals("1")) {
                amount = amount.add(cartItem.getTotalPrice());
            }
        }
        return amount;
    }

    // 商品添加进购物车
    @RequestMapping("/addToCart")
    @AuthRequired(loginMust = false)
    public String addToCart(String skuId, int quantity, HttpServletRequest request, HttpServletResponse response) {
        List<OmsCartItem> omsCartItems = new ArrayList<>();
        // 调用商品服务，查出商品信息
        PmsSkuInfo skuInfo = skuService.getSkuById(skuId);

        // 将商品信息封装成购物车信息
        OmsCartItem cartItem = new OmsCartItem();
        cartItem.setCreateDate(new Date());
        cartItem.setDeleteStatus(0);
        cartItem.setModifyDate(new Date());
        cartItem.setPrice(skuInfo.getPrice());
        cartItem.setProductAttr("");
        cartItem.setProductBrand("");
        cartItem.setProductCatalogId(skuInfo.getCatalog3Id());
        cartItem.setProductId(skuInfo.getSpuId());
        cartItem.setProductName(skuInfo.getSkuName());
        cartItem.setProductPic(skuInfo.getSkuDefaultImg());
        cartItem.setProductSkuCode("11111111111");
        cartItem.setProductSkuId(skuId);
        cartItem.setQuantity(new BigDecimal(quantity));

        // 判断用户是否登录
        String memberId = (String)request.getAttribute("memberId");
        String nickname = (String)request.getAttribute("nickName");

        if (StringUtils.isBlank(memberId)) {
            // 用户没有登录

            // cookie里原有的购物车数据
            String cartListCookie = CookieUtil.getCookieValue(request, "cartListCookie", true);
            // 判断是否有名为cartListCookie的cookie存在
            if (StringUtils.isBlank(cartListCookie)) {
                // cookie为空，则直接添加
                omsCartItems.add(cartItem);
            } else {
                // cookie不为空
                omsCartItems = JSON.parseArray(cartListCookie, OmsCartItem.class);
                // 先判断要添加到cookie中去的购物车数据是否已经在cookie中存在
                boolean exist = ifCurrentItemExistInCookie(omsCartItems, cartItem);
                if (exist) {
                    // 之前添加过，更新购物车数量即可
                    for (OmsCartItem omsCartItem : omsCartItems) {
                        if (omsCartItem.getProductSkuId().equals(cartItem.getProductSkuId())) {
                            omsCartItem.setQuantity(omsCartItem.getQuantity().add(cartItem.getQuantity()));
                            // 这里价格为何如此设置？
//                            omsCartItem.setPrice(omsCartItem.getPrice().add(cartItem.getPrice()));
                        }
                    }
                } else {
                    // 之前没有添加过，新增当前的购物车
                    omsCartItems.add(cartItem);
                }
            }
            CookieUtil.setCookie(request, response, "cartListCookie", JSON.toJSONString(omsCartItems), 60*60*72, true);
        } else {
            // 用户已经登录 Db+cache
            // 从DB中查出购物车数据
            OmsCartItem itemInDb = cartService.getCartItemByUser(memberId, skuId);
            if (itemInDb == null) {
                // 该用户没有添加过当前商品，添加进此用户的购物车
                cartItem.setMemberId(memberId);
                cartItem.setMemberNickname(nickname);
                cartService.addToCart(cartItem);
            } else {
                // 改用户添加过当前商品
                itemInDb.setQuantity(cartItem.getQuantity().add(itemInDb.getQuantity()));
                cartService.updateCart(itemInDb);
            }
            // 同步缓存
            cartService.synchronizeCartCache(memberId);
        }

        return "redirect:/success.html";
    }

    private boolean ifCurrentItemExistInCookie(List<OmsCartItem> omsCartItems, OmsCartItem cartItem){
        boolean exist = false;
        for (OmsCartItem omsCartItem : omsCartItems) {
            String skuId = omsCartItem.getProductSkuId();
            if(skuId.equals(cartItem.getProductSkuId())) {
                exist = true;
            }
        }
        return exist;
    }
}
