package com.hmdp.controller;


import com.hmdp.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;

@Service
@Slf4j
public class Test {
    @GetMapping("/test")
    public Result testJemeter()
    {
       return Result.ok("测试成功");
    }
}
