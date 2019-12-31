package com.atguigu.gmall.interceptors;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.annotations.AuthRequired;
import com.atguigu.gmall.util.CookieUtil;
import com.atguigu.gmall.util.HttpClientUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@Component
public class AuthInterceptor extends HandlerInterceptorAdapter {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception{
        // 拦截代码

        // 判断被拦截请求访问方法的注解(是否是需要拦截的)
        HandlerMethod hm = (HandlerMethod) handler;
        AuthRequired methodAnnotation = hm.getMethodAnnotation(AuthRequired.class);

        StringBuffer urL = request.getRequestURL();
        System.out.println(urL);

        if (methodAnnotation == null) {
            System.out.println("进入拦截器的拦截方法，不需要认证");
            return true;
        }

        System.out.println("进入拦截器的拦截方法，需要认证");

        String token = "";

        String oldToken = CookieUtil.getCookieValue(request, "oldToken", true);
        if (StringUtils.isNotBlank(oldToken)) {
            token = oldToken;
        }
        String newToken = request.getParameter("token");
        if (StringUtils.isNotBlank(newToken)) {
            token = newToken;
        }

        // 调用认证中心验证
        String success = "fail";
        Map<String, String> resultMap = null;
        if (StringUtils.isNotBlank(token)) {
            String ip = request.getHeader("x-forwarded-for"); // 通过nginx转发的客户端ip
            if (StringUtils.isBlank(ip)) {
                ip = request.getRemoteAddr(); // 从request中获取ip
                if (StringUtils.isBlank(ip)) {
                    ip = "127.0.0.1";
                }
            }
            String resultJson = HttpClientUtil.doGet("http://passport.gmall.com:8085/vertify?token=" + token + "&currentIp=" + ip);
            resultMap = JSON.parseObject(resultJson, Map.class);
            success = resultMap.get("status");
        }

        boolean loginMust = methodAnnotation.loginMust();
        if (loginMust) {
            // 必须登录成功才能使用
            if (!success.equals("success")) {
                // 重定向回passport登录
                StringBuffer requestURL = request.getRequestURL();
                response.sendRedirect("http://passport.gmall.com:8085/index?returnUrl=" + requestURL);
                return false;
            } else {
                // 验证通过，覆盖cookie中的token
                if (StringUtils.isNotBlank(token)) {
                    CookieUtil.setCookie(request, response, "oldToken", token, 60*60*2, true);
                }
                // 用户登陆了，需要将token携带的用户信息写入
                request.setAttribute("memberId", resultMap.get("memberId"));
                request.setAttribute("nickname", resultMap.get("nickname"));
            }
        } else {
            // 不用登录也能访问业务，但是必须验证
            if (success.equals("success")) {
                // 用户登陆了，需要将token携带的用户信息写入
                request.setAttribute("memberId", resultMap.get("memberId"));
                request.setAttribute("nickname", resultMap.get("nickname"));
                // 验证通过，覆盖cookie中的token
                if (StringUtils.isNotBlank(token)) {
                    CookieUtil.setCookie(request, response, "oldToken", token, 60*60*2, true);
                }
            }
        }

        return true;
    }
}
