package com.atguigu.gmall.payment.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:alipay.properties")
public class AlipayConfig {

    @Value("${alipay_url}")
    private String alipayUrl;

    @Value("${app_private_key}")
    private String appPrivateKey;

    @Value("${app_id}")
    private String appId;

    public final static String FORMAT = "json";
    public final static String CHARSET = "utf-8";
    public final static String SIGN_TYPE = "RSA2";

    public static String RETRUN_PAYMENT_URL;
    public static String NOTIFY_PAYMENT_URL;
    public static String RETURN_ORDER_URL;
    public static String ALIPAY_PUBLIC_KEY;

    @Value("${alipay_public_key}")
    public void setAlipayPublicKey (String alipayPublicKey){
        AlipayConfig.ALIPAY_PUBLIC_KEY = alipayPublicKey;
    }

    @Value("${return_payment_url}")
    public void setRetrunPaymentUrl (String retrunPaymentUrl) {
        AlipayConfig.RETRUN_PAYMENT_URL = retrunPaymentUrl;
    }

    @Value("${notify_payment_url}")
    public void setNotifyPaymentUrl (String notifyPaymentUrl) {
        AlipayConfig.NOTIFY_PAYMENT_URL = notifyPaymentUrl;
    }

    @Value("${return_order_url}")
    public void setReturnOrderUrl (String returnOrderUrl) {
        AlipayConfig.RETURN_ORDER_URL = returnOrderUrl;
    }

    @Bean
    public AlipayClient alipayClient (){
        DefaultAlipayClient defaultAlipayClient = new DefaultAlipayClient(alipayUrl, appId, appPrivateKey, FORMAT, CHARSET, ALIPAY_PUBLIC_KEY, SIGN_TYPE);
        return defaultAlipayClient;
    }

}
