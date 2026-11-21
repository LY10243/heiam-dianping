package com.hmdp.service.impl;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisData;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static com.hmdp.utils.RedisConstants.*;
/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    //创建独立线程池
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);
    @Resource
    StringRedisTemplate stringRedisTemplate;
    @Override
    public Result  queryById(Long id) {
        // 1 查询Redis
         String shopJson= stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY+id);
         //2判断shop是否为空
        //不为空
       if(StrUtil.isNotBlank(shopJson) )
       {
           //把shop转化成Java
           Shop  shop = JSONUtil.toBean(shopJson, Shop.class);
           return Result.ok(shop);
       }
       if(shopJson!=null) return Result.fail("店铺"+id+"不存在");
        //3 为空
        Shop shopResult = getById(id);
        if(shopResult==null)
       {
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,"",CACHE_NULL_TTL, TimeUnit.MINUTES);
           return Result.fail("商品"+id+"不存在");
       }
       //把 shop转为JSON进行存储
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id, JSONUtil.toJsonStr(shopResult)+RandomUtil.randomInt(), CACHE_SHOP_TTL, TimeUnit.MINUTES);
        return Result.ok(shopResult);
    }
     private  boolean lock(String key)
    {
        Boolean flag= stringRedisTemplate.opsForValue().setIfAbsent(key, "1", LOCK_SHOP_TTL, TimeUnit.MINUTES);
        //使用包装类
        return BooleanUtil.isTrue(flag);
    }
    private  void ReverseLock(String key)
    {
      stringRedisTemplate.delete(key);
    }
    @Transactional
    @Override
    public Result update(Shop shop) {

       //1 更新数据库
        updateById(shop);

        //2 删除缓存
        Long id = shop.getId();
        if(id==null)   return Result.fail("店铺 id 不存在");
        stringRedisTemplate.delete(CACHE_SHOP_KEY+id);
        return Result.ok();
    }
    public Result queryId(Long id) throws InterruptedException {
        //缓存穿透
        Shop shop = queryWithThroughLock(id);
        if(shop==null)  return Result.fail("店铺不存在");return Result.ok(shop);
    }
    public Shop queryWithThroughLock(Long id) throws InterruptedException {
        // 1 查询Redis
        String shopJson= stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY+id);
        //2判断shop是否为空
        //不为空
        if(StrUtil.isNotBlank(shopJson) )
        {
            //把shop转化成Java
            return  JSONUtil.toBean(shopJson, Shop.class);
        }
        if(shopJson!=null) return  null;
        //3 为空
        //获取互斥锁
        String key=LOCK_SHOP_KEY+id;
        boolean lock = lock(key);
        //判断是否获取成功
        if(!lock)
        {
            //休眠
            Thread.sleep(50);
            //做递归重新执行
            queryWithThroughLock(id);
        }
        //3.2 查询数据库
        Shop shopResult = getById(id);
        if(shopResult==null) stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,"",CACHE_NULL_TTL, TimeUnit.MINUTES);
        //把 shop转为JSON进行存储
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,
                JSONUtil.toJsonStr(shopResult)+RandomUtil.randomInt(),
                CACHE_SHOP_TTL, TimeUnit.MINUTES);
        //释放互斥锁
        ReverseLock(LOCK_SHOP_KEY);
        return  shopResult;
    }

     public void saveShopRedis(Long id,Long expireSecond) throws InterruptedException {
        RedisData redisData=new RedisData();

        //查询店铺数据
        Shop  shop = getById(id);
        Thread.sleep(50);
        redisData.setData(shop);
        //封装过期时间
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSecond));
        //写入Redis
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,JSONUtil.toJsonStr(redisData));
    }



    public Result queryLogic(Long id)  {
       //用逻辑过期解决问题
        Shop shop =  queryWithThroughLogic(id);
        if(shop==null)  return Result.fail("店铺不存在");return Result.ok(shop);
    }
    public Shop queryWithThroughLogic(Long id)   {
        // 1 查询Redis
        String shopJson= stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY+id);
        //2判断shop是否为空
        //不为空
        if(StrUtil.isBlank(shopJson) )
        {
            //把shop转化成Java
            return  null;
        }
        //命中
        //反序列化为对象
        RedisData redisData=JSONUtil.toBean(shopJson,RedisData.class);
        //判断时间是否过期
        //在过期时间之前
        if(LocalDateTime.now().isBefore(redisData.getExpireTime())) return  (Shop) redisData.getData();
        //过期 缓存重建
        //获取互斥锁
        boolean lock = lock(CACHE_SHOP_KEY + id);
        //失败
        if(!lock)  return  (Shop) redisData.getData();
        //开启独立线程
        CACHE_REBUILD_EXECUTOR.submit(() -> {
            try {
                this.saveShopRedis(id, 20L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            finally {
                // 释放锁
                ReverseLock(CACHE_SHOP_KEY + id);
            }
        });
        return (Shop) redisData.getData();

    }
}
