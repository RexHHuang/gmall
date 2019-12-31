package com.atguigu.gmall.user.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class UserController {

    @Reference
    private UserService userService;

    @ResponseBody
    @RequestMapping("/getAllUser")
    public List<UmsMember> getAllUser() {
        List<UmsMember> umsMembers = userService.getAllUSer();
        return umsMembers;
    }
}
