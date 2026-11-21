package com.hmdp.utils;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RedisData {
    //过期时间
    private LocalDateTime expireTime;
    //万能存储数据对象
    private Object data;
}
