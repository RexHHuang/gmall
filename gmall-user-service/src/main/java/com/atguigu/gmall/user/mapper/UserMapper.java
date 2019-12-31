package com.atguigu.gmall.user.mapper;

import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.bean.UmsMemberReceiveAddress;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface UserMapper extends Mapper<UmsMember> {

    List<UmsMember> selectAllUser();

    @Select("select * from ums_member_receive_address where member_id = #{memberId}")
    List<UmsMemberReceiveAddress> selectReceiveAddressByMemberId( @Param("memberId") String memberId);

    @Select("select * from ums_member_receive_address where id = #{receiveAddressId}")
    UmsMemberReceiveAddress selectAddressById(@Param("receiveAddressId") String receiveAddressId);
}
