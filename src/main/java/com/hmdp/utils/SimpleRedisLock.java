package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements RedisLock{
    private StringRedisTemplate stringRedisTemplate;
    private static final String String_Pre="lock:";
    private   String name;

    public SimpleRedisLock(StringRedisTemplate stringRedisTemplate, String name) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.name = name;
    }

    @Override
    public boolean tryLock(long timeout) {
        long id = Thread.currentThread().getId();
        Boolean b = stringRedisTemplate.opsForValue()
                .setIfAbsent(String_Pre + name,"线程:"+id , timeout, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(b);
    }
    @Override
    public void ReverseLock() {
        stringRedisTemplate.delete(String_Pre + name);
    }
}
