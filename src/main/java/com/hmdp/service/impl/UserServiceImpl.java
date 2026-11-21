package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;
import static com.hmdp.utils.RegexUtils.isPhoneInvalid;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Resource
    StringRedisTemplate stringRedisTemplate;
    @Override
    public Result sendCode(String phone, HttpSession session) {
        //1  检验手机号
        if(isPhoneInvalid(phone))
        {
            return Result.fail("手机号非法");
        }
        //2 生成验证码
        String code = RandomUtil.randomString(6);
        System.out.println("验证码是:"+code);
        //3 TODO 保存验证码
        //Redi是共享数据空间，会导致数据被覆盖
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY+phone,code,LOGIN_CODE_TTL, TimeUnit.MINUTES);
        return Result.ok();
    }

    @Override
    public Result Login(LoginFormDTO loginForm, HttpSession session) {
        //1  检验手机号
        String phone = loginForm.getPhone();
        if(isPhoneInvalid(phone))
        {
            return Result.fail("手机号非法");
        }
        //2 校验验证码
        //2。1  获取用户输入验证码
        String code = loginForm.getCode();
        //2.2    Redis 拿到验证码
        String s = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        //2.3验证码是否正确
        if(s==null || !s.equals(code))
        {
         return    Result.fail("验证码错误");

        }
        //3 检验用户是否存在
       User user = query().eq("phone", phone).one();
        //不存在
        if(user==null)
        {
            user=new User();
            user.setUpdateTime(LocalDateTime.now());
            user.setPhone(phone);
            user.setUpdateTime(LocalDateTime.now());
            save(user);
        }
       //用户保存到redis当中
        //随机token ，生成令牌
        String token= UUID.randomUUID().toString();
        //把User 转化为Hash存储
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> tempMap = BeanUtil.beanToMap(userDTO,new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
// 把id 的 Long类型转化为 String
                        .setFieldValueEditor((filedName,fliedValue)->
                                fliedValue.toString()));

// 存入redis用这个hashMap
        stringRedisTemplate.opsForHash().putAll(LOGIN_USER_KEY + token, tempMap);
        // 返回Token
        return Result.ok(token);
    }
}
