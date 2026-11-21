package com.hmdp.service;

public interface ILock {
  boolean  lock(long timeout);
  void Reverse();
}
