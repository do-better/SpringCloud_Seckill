package com.imooc.order.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RedisService {

    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    public Object get(String key) {
        return key == null ? null : redisTemplate.opsForValue().get(key);
    }

    public boolean set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception e) {
            log.error("redis set异常", e);
            return false;
        }
    }

    /**
     * 带过期时间的
     * @param key
     * @param value
     * @param timeout
     * @return
     */
    public boolean setex(String key, Object value, long timeout) {
        try {
            //单位是毫秒
            redisTemplate.opsForValue().set(key, value, timeout, TimeUnit.SECONDS);
            return true;
        } catch (Exception e) {
            log.error("redis setex异常", e);
            return false;
        }
    }

    public Long decr(String key, long delta) {
        if (delta > 0) {
            throw new RuntimeException("递减因子必须小于0");
        }
        return redisTemplate.opsForValue().increment(key, delta);
    }
}
