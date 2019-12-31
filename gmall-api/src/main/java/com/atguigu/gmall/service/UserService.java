package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.bean.UmsMemberReceiveAddress;

import java.util.List;

public interface UserService {

    List<UmsMember> getAllUSer();

    UmsMember login(UmsMember umsMember);

    void addUserTokenToCache(String token, String memberId);

    void addOauthUser(UmsMember umsMember);

    UmsMember checkOauthUser(String sourceUid);

    UmsMember getOauthUser(String sourceUid);

    UmsMember checkAndGetOauthUser(String sourceUid);

    List<UmsMemberReceiveAddress> getReceiveAddressByMemberId(String memberId);

    UmsMemberReceiveAddress getReceiveAddressById(String receiveAddressId);
}
