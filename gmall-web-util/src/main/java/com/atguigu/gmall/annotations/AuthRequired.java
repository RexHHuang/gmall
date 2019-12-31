package com.atguigu.gmall.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthRequired {

    /**
     *
     * 为 true 的时候，需要认证，且必须要登录之后才能访问某个方法
     * 为 false 的时候，需要认证，但是有的方法不登录也可以访问，比如购物车里面的方法，认证是看他
     * 登录没登录，不同的状态，购物车要走不同的分支 (比如是走cookie分支，还是走数据库分支)
     */
    boolean loginMust() default true;
}
