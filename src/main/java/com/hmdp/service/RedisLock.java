package com.hmdp.service;

import cn.hutool.core.util.BooleanUtil;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class RedisLock implements ILock{

    private StringRedisTemplate stringRedisTemplate;
    private String name;
    private static  final String key_pre="lock:";
//    private static final RedisScript<Long> UNLOCK= RedisScript.of(new ClassPathResource("unlock.lua"),Long.class);
    private static final String ID_pre=UUID.randomUUID().toString()+"-";
    public RedisLock(StringRedisTemplate stringRedisTemplate, String name) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.name = name;
    }

    @Override
    public boolean lock(long timeout) {
        String threadID = Thread.currentThread().getId() + ID_pre;
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(key_pre + name, threadID, timeout, TimeUnit.MINUTES);
        return BooleanUtil.isTrue(success);
    }
    @Override
    public void Reverse() {
        //获取线程
        String Thread = stringRedisTemplate.opsForValue().get(key_pre + name);
        if(Thread.equals(ID_pre)) stringRedisTemplate.delete(key_pre + name);

//        String threadId = ID_pre + Thread.currentThread().getId();
//        String lockKey = key_pre + name;
//        stringRedisTemplate.execute(UNLOCK,
//                Collections.singletonList(lockKey),
//                threadId);
    }
    }
