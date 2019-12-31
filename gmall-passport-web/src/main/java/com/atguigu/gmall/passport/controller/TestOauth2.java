package com.atguigu.gmall.passport.controller;

import com.atguigu.gmall.util.HttpClientUtil;

import java.util.HashMap;
import java.util.Map;

public class TestOauth2 {
    public static void main(String[] args) {
//        String s1 = HttpClientUtil.doGet("https://api.weibo.com/oauth2/authorize?client_id=321623546&response_type=code&redirect_uri=http://passport.gmall.com:8085/vlogIn");
//        System.out.println(s1);
//        String s2 = "http://passport.gmall.com:8085/vlogIn?code=bfcdbaff40704514a62023b9052a4d39";
//        321623546
//        92420cc37134dade404448a0a22edfa5
        String s3 = "https://api.weibo.com/oauth2/access_token";
        Map<String, String> parms = new HashMap<>();
        parms.put("client_id", "321623546");
        parms.put("client_secret", "92420cc37134dade404448a0a22edfa5");
        parms.put("grant_type", "authorization_code");
        parms.put("redirect_uri", "http://passport.gmall.com:8085/vlogIn");
        parms.put("code", "2cd96e30c552a00ffba09a1035865352");

        String access_token = HttpClientUtil.doPost(s3, parms);
        System.out.println(access_token);

    }
}
