package com.hmdp.utils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class RedisId{
    private static final long Begin_TIMESTAMP=1640995200;
    @Resource
    StringRedisTemplate stringRedisTemplate;

    public RedisId(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }
    public long nextId(String key)
    {

        //1 时间戳  当前时间-起始时间
        LocalDateTime now = LocalDateTime.now();
        long epochSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timestamp = epochSecond - Begin_TIMESTAMP;
        //2 序列号
        //获取日期到天,获取日期到天，方便统计
        String yyyyMMdd = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        long increment = stringRedisTemplate.opsForValue().increment("icr:"+key+":"+yyyyMMdd);
        //timestamp<<32  : 确保时间戳放在最高位
        return timestamp<<32|increment;
    }

}
