package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.UpdateChainWrapper;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.User;
import com.hmdp.entity.Voucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.ReactiveSubscription;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

import java.util.concurrent.CountDownLatch;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service

public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Resource
    RedisId redisId;
    @Autowired
    ISeckillVoucherService seckillVoucherService;
    @Resource
    StringRedisTemplate stringRedisTemplate;
    @Override
    public Result seckillVoucher(Long voucherId) throws InterruptedException {
        //查看用户是否登录
        UserDTO user = UserHolder.getUser();
        if(user==null) return Result.fail("用户未登录");
        //获取优惠卷 ID
        SeckillVoucher voucher  = seckillVoucherService.getById(voucherId);
        //判断时间
        if(LocalDateTime.now().isBefore(voucher.getBeginTime())) return Result.fail("售卖时间未开始");
        if(LocalDateTime.now().isAfter(voucher.getEndTime())) return Result.fail("售卖时间已结束");
        //判断库存
        Integer stock = voucher.getStock();
        if(stock <0) return Result.fail("优惠卷已卖光");
        //释放锁的时机和食物是否生效的问题如何解决
        //保证线程安全
        synchronized (user.getId().toString().intern()) {
//创建锁对象
            SimpleRedisLock simpleRedisLock=new SimpleRedisLock(stringRedisTemplate,"ORDER:"+user.getId());
            boolean lock = simpleRedisLock.tryLock(1200);
            if(!lock)
            {
                return Result.fail("一个人只允许一单");
            }
            //当前对象的代理对象,这样的话直接由Spring提交
            //加入ascept依赖,在启动类里面加上
      try{
          IVoucherOrderService  o = (IVoucherOrderService) AopContext.currentProxy();
          return o.getResult(voucherId);
      }
      finally {
          simpleRedisLock.ReverseLock();
      }
        }
//        synchronized (user.getId().toString().intern()) {
//            //直接这么写的话 this. getResult(voucherId);
//            Object o = AopContext.currentProxy();
//            return  getResult(voucherId);
//        }
    }
    @Transactional
    public  Result getResult(Long voucherId)
    {
        UserDTO user = UserHolder.getUser();
        //id.toString()不行，因为每次调用toString()方法都会new 出来一个对象
        //锁的是相同的用户

            //判断用户是否下过单
            Integer count = query().eq("user_id", user.getId()).eq("voucher_id", voucherId).count();
            if(count>0) return Result.fail("您已下过订单") ;
            //扣库存(乐观锁只能用于插入数据）
            boolean result = seckillVoucherService.update().
                    setSql("stock=stock-1").//setSql(执行的sql)
                            eq("voucher_id", voucherId).//eq()条件判断
                            gt("stock", 0).
                    update();
            //创建订单
            if(!result) return Result.fail("订单创建失败");
            VoucherOrder voucherOrder=new VoucherOrder();
            voucherOrder.setId(redisId.nextId("order"));
            voucherOrder.setVoucherId(voucherId);
            voucherOrder.setUserId(UserHolder.getUser().getId());
            save(voucherOrder);
            return Result.ok(voucherOrder);
        }
    @Transactional
    //问题：当有多个JVM同时进行的时候，会扣除多个订单（同一个用户）
    public Result createVoucherOrder(Long voucherId)
    {
       return Result.ok();
    }
    }
