package com.hmdp.utils;

public interface RedisLock {
    boolean tryLock(long timeout);
    void ReverseLock();
}
