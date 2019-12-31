package com.atguigu.gmall.seckill.controller;

import com.atguigu.gmall.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.List;

@Controller
public class SeckillController {

    @Autowired
    RedisUtil redisUtil;

    /**
     * apache 压力测试命令 ab -n xxx -c xxx "http://localhost:8001/kill"
     * -n 请求次数
     * -c 并发数
     * @return
     */
    @RequestMapping("/kill")
    @ResponseBody
    public String kill() {
        Jedis jedis = redisUtil.getJedis();

        // 开启商品的监控
        jedis.watch("106");

        Integer stock = Integer.parseInt(jedis.get("106"));
        if (stock > 0) {
            Transaction multi = jedis.multi();
            multi.incrBy("106", -1);
            // 某个线程如果在这停顿住，被别的线程抢先提交，这次线程的提交结果为null
            List<Object> exec = multi.exec();
            if (exec != null && exec.size() > 0)
                System.out.println("当前库存：" + stock + "，某用户抢购成功");
            else
                System.out.println("当前库存剩余数量：" + stock + "，某用户抢购失败");
        }
        jedis.close();
        return "1";
    }
}
