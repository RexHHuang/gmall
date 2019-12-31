package com.atguigu.gmall.user.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.bean.UmsMemberReceiveAddress;
import com.atguigu.gmall.service.UserService;
import com.atguigu.gmall.user.mapper.UserMapper;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    RedisUtil redisUtil;

    @Override
    public List<UmsMember> getAllUSer() {
        return userMapper.selectAll();
    }

    @Override
    public UmsMember login(UmsMember umsMember) {
        Jedis jedis = null;
        try {
            jedis = redisUtil.getJedis();
            if (jedis != null) {
                String umsMemberStr = jedis.get("user:" + umsMember.getUsername() + ":" + umsMember.getPassword() + ":info");
                if (StringUtils.isNotBlank(umsMemberStr)) {
                    // 说明用户名和密码正确
                    UmsMember umsMemberFromCache = JSON.parseObject(umsMemberStr, UmsMember.class);
                    return umsMemberFromCache;
                } else {
                    // 用户名或者密码错误，或者缓存中没有改用户的数据
                    // 开启数据库
                    UmsMember umsMemberFromDb = selectFromDatabase(umsMember);
                    if (umsMemberFromDb != null) {
                        jedis.setex("user:" + umsMember.getUsername() + ":" + umsMember.getPassword() + ":info",
                                60*60*24, JSON.toJSONString(umsMemberFromDb));
                    }
                    return umsMemberFromDb;
                }

            } else {
                // 如果jedis没有连接上，开启数据库
                UmsMember umsMemberFromDb = selectFromDatabase(umsMember);
                return umsMemberFromDb;
            }
        } finally {
            jedis.close();
        }

    }


    public UmsMember selectFromDatabase(UmsMember umsMember){
        return userMapper.selectOne(umsMember);
    }

    @Override
    public void addUserTokenToCache(String token, String memberId) {
        Jedis jedis = redisUtil.getJedis();
        jedis.setex("user:" + memberId + ":token", 60*60*2, token);

        jedis.close();
    }

    @Override
    public void addOauthUser(UmsMember umsMember) {
        userMapper.insertSelective(umsMember);
    }

    @Override
    public UmsMember checkOauthUser(String sourceUid) {
        Example e = new Example(UmsMember.class);
        e.createCriteria().andEqualTo("sourceUid", sourceUid);
        return userMapper.selectOneByExample(e);
    }

    @Override
    public UmsMember getOauthUser(String sourceUid) {
        Example example = new Example(UmsMember.class);
        example.createCriteria().andEqualTo("sourceUid", sourceUid);
        return userMapper.selectOneByExample(example);
    }

    @Override
    public UmsMember checkAndGetOauthUser(String sourceUid) {
        Example example = new Example(UmsMember.class);
        example.createCriteria().andEqualTo("sourceUid", sourceUid);
        return userMapper.selectOneByExample(example);
    }

    @Override
    public List<UmsMemberReceiveAddress> getReceiveAddressByMemberId(String memberId) {
       return userMapper.selectReceiveAddressByMemberId(memberId);
    }

    @Override
    public UmsMemberReceiveAddress getReceiveAddressById(String receiveAddressId) {

        return userMapper.selectAddressById(receiveAddressId);
    }

}
