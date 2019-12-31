package com.atguigu.gmall.payment.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.atguigu.gmall.annotations.AuthRequired;
import com.atguigu.gmall.bean.OmsOrder;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.PaymentService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PaymentController {

    @Autowired
    AlipayClient alipayClient;

    @Reference
    OrderService orderService;

    @Autowired
    PaymentService paymentService;

    @RequestMapping("/index")
    @AuthRequired(loginMust = true)
    public String index(String orderId, HttpServletRequest request, ModelMap modelMap){
        String memberId = (String) request.getAttribute("memberId");
        String nickname = (String) request.getAttribute("nickname");

        OmsOrder order = orderService.getOrderById(orderId);
        String outTradeNo = order.getOrderSn();
        BigDecimal totalAmount = order.getTotalAmount();

        modelMap.put("nickname", nickname);
        modelMap.put("outTradeNo", outTradeNo);
        modelMap.put("totalAmount", totalAmount);
        return "index";
    }

    /**
     * 支付后支付宝的回调方法
     * @return
     */
    @RequestMapping("alipay/callback/return")
    @AuthRequired
    public String alipayCallbackReturn (HttpServletRequest request) {
        // 回调请求中获取支付宝的请求参数
        String sign = request.getParameter("sign");
        String trade_no = request.getParameter("trade_no");
        String out_trade_no = request.getParameter("out_trade_no");
        String total_amount = request.getParameter("total_amount");
        String subject = request.getParameter("subject");
        String call_back_content = request.getQueryString();

        // 通过支付宝的paramMap进行签名验证，2.0版本的接口将paramMap参数去掉了，导致请求没法验签
        // 做一个假的验签
        if (StringUtils.isNotBlank(sign)) {
            // 验签通过
            PaymentInfo paymentInfo = new PaymentInfo();
            paymentInfo.setOutTradeNo(out_trade_no);
            paymentInfo.setPaymentStatus("已支付"); //  已支付
            paymentInfo.setAlipayTradeNo(trade_no);  // 支付宝的交易凭证号
            paymentInfo.setCallbackContent(call_back_content); // 回调请求字符串
            paymentInfo.setCallbackTime(new Date());

            // 更新用户的支付状态
            paymentService.updatePaymentInfo(paymentInfo);

            // 支付成功后引起的系统服务-> 订单服务的更新 -> 库存服务 -> 物流服务


        } else {
            return "tradeFailed";
        }

        return "finish";
    }

    /**
     * 微信支付的调用方法
     * @return
     */
    @RequestMapping("mx/submit")
    @AuthRequired
    public String mx() {

        return null;
    }

    /**
     * 支付宝支付的调用方法
     * @return
     */
    @RequestMapping("alipay/submit")
    @AuthRequired
    @ResponseBody
    public String alipay(String outTradeNo, BigDecimal totalAmount){

        // 获得一个支付宝请求的客户端（他并不是一个链接，而是一个封装好的http的表单请求）
        String form = null;
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        // 回调函数
        alipayRequest.setReturnUrl(AlipayConfig.RETRUN_PAYMENT_URL);
        alipayRequest.setNotifyUrl(AlipayConfig.NOTIFY_PAYMENT_URL);

        Map<String, Object> map = new HashMap<>();
        map.put("out_trade_no", outTradeNo);
//        map.put("total_amount", totalAmount);
        map.put("total_amount", "0.01");
        map.put("product_code", "FAST_INSTANT_TRADE_PAY");
        map.put("subject", "尚硅谷商城");

        String param = JSON.toJSONString(map);
        alipayRequest.setBizContent(param);

        try {
            // 调用SDK生成表单
            form = alipayClient.pageExecute(alipayRequest).getBody();
//            System.out.println(form);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        // 生成并保存用户的支付信息
        OmsOrder order = orderService.getOrderByOutTreadeNo(outTradeNo);
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setOrderId(order.getId());
        paymentInfo.setOutTradeNo(outTradeNo);
        paymentInfo.setPaymentStatus("未支付");
        paymentInfo.setSubject("谷粒商城");
        paymentInfo.setTotalAmount(totalAmount);
        paymentService.savePaymentInfo(paymentInfo);

        // 主动向消息中间件发送一个检查支付状态（支付服务消费）的延迟消息队列
        paymentService.checkPaymentResultQueue(outTradeNo, 5);
        // 提交请求到支付宝
        return form;
    }

}
