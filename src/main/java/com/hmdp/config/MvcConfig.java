package com.hmdp.config;

import com.hmdp.utils.LoginInteruptter;
import com.hmdp.utils.RedisTokenInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

//由Spring
@Configuration
public class MvcConfig implements WebMvcConfigurer {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //全部请求
        registry.addInterceptor(new RedisTokenInterceptor(stringRedisTemplate)).order(2);
        //部分请求
        registry.addInterceptor(new LoginInteruptter())
                .excludePathPatterns(
                        "/blog/hot",
                        "/shop/**",
                        "/shop-type/**",
                        "/user/me",
                        "/user/code",
                        "/user/login",
                        "/voucher/**"
                ).order(0);
    }
}