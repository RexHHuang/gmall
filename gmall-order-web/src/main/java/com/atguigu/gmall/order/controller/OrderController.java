package com.atguigu.gmall.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.annotations.AuthRequired;
import com.atguigu.gmall.bean.OmsCartItem;
import com.atguigu.gmall.bean.OmsOrder;
import com.atguigu.gmall.bean.OmsOrderItem;
import com.atguigu.gmall.bean.UmsMemberReceiveAddress;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Controller
public class OrderController {

    @Reference
    CartService cartService;

    @Reference
    UserService userService;

    @Reference
    OrderService orderService;

    @Reference
    SkuService skuService;

    @RequestMapping("submitOrder")
    @AuthRequired(loginMust = true)
    public ModelAndView submitOrder(String receiveAddressId, BigDecimal totalAmount, String tradeCode, HttpServletRequest request) {
        String memberId = (String)request.getAttribute("memberId");
        String nickname = (String)request.getAttribute("nickname");
        ModelAndView mv = new ModelAndView("tradeFail");
        // 检查交易码
        boolean success = orderService.checkTradeCode(memberId, tradeCode);
        if (success) {
            List<OmsOrderItem> orderItems = new ArrayList<>();
            // 订单对象
            OmsOrder order = new OmsOrder();
            order.setAutoConfirmDay(3); // 几天之后自动确认收货
            order.setCreateTime(new Date());
            order.setMemberId(memberId);
            order.setMemberUsername(nickname);
            order.setNote("请快速发货"); // 订单备注
            String outTradeNo = "gmall";
            outTradeNo += System.currentTimeMillis(); // 将毫秒时间戳拼接到外部订单号
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("YYYYMMDDHHmmss");
            outTradeNo += simpleDateFormat.format(new Date()); // 将时间字符串拼接到外部订单号
            order.setOrderSn(outTradeNo); // 外部订单号
            order.setOrderType(1);
            UmsMemberReceiveAddress receiveAddress = userService.getReceiveAddressById(receiveAddressId);
            order.setReceiverDetailAddress(receiveAddress.getDetailAddress());
            order.setReceiverName(receiveAddress.getName());
            order.setReceiverPhone(receiveAddress.getPhoneNumber());
            order.setReceiverPostCode(receiveAddress.getPostCode());
            order.setReceiverProvince(receiveAddress.getProvince());
            order.setReceiverCity(receiveAddress.getCity());
            order.setReceiverRegion(receiveAddress.getRegion());
            // 当前日期加一天，一天后配送至
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DATE, 1);
            Date time = calendar.getTime();
            order.setReceiveTime(time);
            order.setSourceType(0); // 0 pc订单 1 app订单
            order.setStatus("0"); // 待付款
            order.setTotalAmount(totalAmount);

            // 根据用户id获取要购买的商品列表（从购物车中），和总价格
            List<OmsCartItem> cartItems = cartService.cartList(memberId);
            for (OmsCartItem cartItem : cartItems) {
                if (cartItem.getIsChecked().equals("1")) {
                    // 获得订单详情列表
                    OmsOrderItem orderItem = new OmsOrderItem();
                    // 验证价格
                    boolean right = skuService.checkPrice(cartItem.getProductSkuId(), cartItem.getPrice());
                    if (!right) {
                        return mv;
                    }
                    // 验证库存，远程调用库存系统

                    orderItem.setProductPrice(cartItem.getPrice());
                    orderItem.setProductName(cartItem.getProductName());

                    orderItem.setOrderSn(outTradeNo); // 外部订单号，用来和其他系统交互，防止重复
                    orderItem.setProductPic(cartItem.getProductPic());
                    orderItem.setProductCategoryId(cartItem.getProductCatalogId());
                    orderItem.setRealAmount(cartItem.getTotalPrice());
                    orderItem.setProductQuantity(cartItem.getQuantity());
                    orderItem.setProductSkuCode("1111111"); // 商品条形码
                    orderItem.setProductSkuId(cartItem.getProductSkuId());
                    orderItem.setProductId(cartItem.getProductId());
                    orderItem.setProductSn("repositryNo1"); // 仓库中的skuId

                    orderItems.add(orderItem);
                }
            }
            order.setPayAmount(getAmount(cartItems)); // 支付总金额
            order.setOmsOrderItems(orderItems);

            // 将订单和订单详情写入数据库，删除购物车中对应的商品
            String orderId = orderService.saveOrder(order);

            // 重定向到支付系统
            mv.setViewName("redirect:http://payment.gmall.com:8087/index");
//            mv.addObject("outTradeNo", outTradeNo);
//            mv.addObject("totalAmount", totalAmount);
            mv.addObject("orderId", orderId);
            return mv;
        } else {
            return mv;
        }
    }

    @RequestMapping("/toTrade")
    @AuthRequired(loginMust = true)
    public String toTrade(ModelMap modelMap, HttpServletRequest request, HttpServletResponse response){
        String memberId = (String)request.getAttribute("memberId");
        String nickname = (String)request.getAttribute("nickname");

        // 收件人地址列表
        List<UmsMemberReceiveAddress> addressList = userService.getReceiveAddressByMemberId(memberId);

        // 将购物车集合转化为页面清单集合
        List<OmsCartItem> cartItems = cartService.cartList(memberId);

        List<OmsOrderItem> orderItems = new ArrayList<>();
        for (OmsCartItem cartItem : cartItems) {
            // 每循环一个购物车对象，就封装一个商品的详情到 OmsOrderItem
            if (cartItem.getIsChecked().equals("1")) {
                OmsOrderItem orderItem = new OmsOrderItem();
                orderItem.setProductName(cartItem.getProductName());
                orderItem.setProductPic(cartItem.getProductPic());
                orderItem.setProductQuantity(cartItem.getQuantity());
                orderItems.add(orderItem);
            }
        }

        modelMap.put("orderItems", orderItems);
        modelMap.put("userAddressList", addressList);
        modelMap.put("totalAmount", getAmount(cartItems));

        // 生成交易码
        String tradeCode = orderService.generateTradeCode(memberId);
        modelMap.put("tradeCode", tradeCode);
        return "trade";

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
}
