package com.atguigu.gmall.passport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.service.UserService;
import com.atguigu.gmall.util.HttpClientUtil;
import com.atguigu.gmall.util.JwtUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PassportController {

    @Reference
    UserService userService;

    @RequestMapping("/vlogIn")
    public String vlogIn(String code, HttpServletRequest request){
        // 授权码换取access_token
        String accessTokenUrl = "https://api.weibo.com/oauth2/access_token";
        Map<String, String> parms = new HashMap<>();
        parms.put("client_id", "321623546");
        parms.put("client_secret", "92420cc37134dade404448a0a22edfa5");
        parms.put("grant_type", "authorization_code");
        parms.put("redirect_uri", "http://passport.gmall.com:8085/vlogIn");
        parms.put("code", code);
        String accessTokenJson = HttpClientUtil.doPost(accessTokenUrl, parms);
        Map<String, String> accessTokenMap = JSON.parseObject(accessTokenJson, Map.class);

        // access_token 换取用户信息
        String uid = accessTokenMap.get("uid");
        String accessToken = accessTokenMap.get("access_token");
        String userUrl = "https://api.weibo.com/2/users/show.json?access_token=" + accessToken + "&uid=" + uid;
        String userJson = HttpClientUtil.doGet(userUrl);
        // 将用户信息保存数据库，用户类型设置为微博用户
        Map<String, String> userMap = JSON.parseObject(userJson, Map.class);
        UmsMember umsMember = new UmsMember();
        umsMember.setSourceType(2);
        umsMember.setAccessCode(code);
        umsMember.setAccessToken(accessToken);
        umsMember.setSourceUid(String.valueOf(userMap.get("id")));
        umsMember.setCity(userMap.get("location"));
        umsMember.setNickname(userMap.get("screen_name"));
        umsMember.setUsername(userMap.get("screen_name"));

        if (userMap.get("gender").equals("m"))
            umsMember.setGender(1);
        else
            umsMember.setGender(2);

        // 检查社交用户以前是否登陆过系统
        UmsMember umsMemberCheck = userService.checkOauthUser(umsMember.getSourceUid());

        if (umsMemberCheck == null) {
            userService.addOauthUser(umsMember);
            // 再次获取umsMember 也可以在上面一句代码中 让 addOauthUser 方法把umsMember返回来，这样根据主键生成策略，也会有 id
            umsMember = userService.getOauthUser(umsMember.getSourceUid());
        } else {
            umsMember = umsMemberCheck;
        }

        // 生成jwt的token，并且重定向到首页，携带该token
        String memberId = umsMember.getId();
        String nickname = umsMember.getNickname();
        String token = generateToken(memberId, nickname, request);
        // token 存入redis一份
        userService.addUserTokenToCache(token, memberId);


        return "redirect:http://search.gmall.com:8083/index?token=" + token;
    }

    @RequestMapping("/vertify")
    @ResponseBody
    public String vertify(String token,String currentIp){

        // 通过jwt校验token的真假
        Map<String, String> resultMap = new HashMap<>();

        Map<String, Object> decodeMap = JwtUtil.decode(token, "2019gmall", currentIp);
        if (decodeMap != null) {
            resultMap.put("status", "success");
            resultMap.put("memberId", (String) decodeMap.get("memberId"));
            resultMap.put("nickname", (String) decodeMap.get("nickname"));
        } else {
            resultMap.put("status", "fail");
        }

        return JSON.toJSONString(resultMap);
    }

    @RequestMapping("/login")
    @ResponseBody
    public String login(UmsMember umsMember, HttpServletRequest request){
        String token = "";

        // 调用用户服务验证用户名和密码
        UmsMember umsMemberLogin = userService.login(umsMember);
        if (umsMemberLogin != null) {
            // 登录成功
            // 用jwt制作token
            String memberId = umsMemberLogin.getId();
            String nickname = umsMemberLogin.getNickname();
            token = generateToken(memberId, nickname, request);

            // 将token存入redis一份
            userService.addUserTokenToCache(token, memberId);
        } else {
            // 登录失败，给前端一个fail标志，告诉前端登录失败
            token = "fail";
        }

        return token;
    }

    @RequestMapping("/index")
    public String index(String returnUrl, ModelMap modelMap){
        modelMap.put("returnUrl", returnUrl);
        return "index";
    }

    private String generateToken(String memberId, String nickname, HttpServletRequest request){
        String token;
        Map<String, Object> map = new HashMap<>();
        map.put("memberId", memberId);
        map.put("nickname", nickname);

        String ip = request.getHeader("x-forwarded-for"); // 通过nginx转发的客户端ip
        if (StringUtils.isBlank(ip)) {
            ip = request.getRemoteAddr(); // 从request中获取ip
            if (StringUtils.isBlank(ip)) {
                ip = "127.0.0.1";
            }
        }

        token = JwtUtil.encode("2019gmall", map, ip);
        return token;
    }
}
