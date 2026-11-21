package com.hmdp;

import com.hmdp.dto.UserDTO;
import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.junit.jupiter.api.Test;
import java.util.concurrent.CountDownLatch;

import javax.annotation.Resource;

@SpringBootTest
class HmDianPingApplicationTests {
    @Autowired
    ShopServiceImpl shopService;
@Test
  public void test() throws InterruptedException {
    // 模拟5个并发请求线程，同一个用户id=1012L
    CountDownLatch latch = new CountDownLatch(5);

    for (int i = 0; i < 5; i++) {
        new Thread(() -> {
            // 模拟拦截器：每个请求线程里面拿到userId
            // 重点：每次getUser().getId() 得到的Long，有可能是新对象！
            UserDTO userDTO = new UserDTO();
            userDTO.setId(1012L);
            UserHolder.saveUser(userDTO);

            Long userId = UserHolder.getUser().getId();

            // ✅ 打印【值】，全部都是1012，看起来一样
            System.out.println("userId数值：" + userId);
            // ✅ 打印【对象的内存标识】！这个才是synchronized认的东西
            System.out.println("==== 对象identityHashCode：" + System.identityHashCode(userId));

            // 尝试锁这个userId
            synchronized (userId) {
                System.out.println(Thread.currentThread().getName() + " 获取到锁，执行业务");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println(Thread.currentThread().getName() + " 释放锁");
            }

            UserHolder.removeUser();
            latch.countDown();
        }, "线程-" + i).start();
    }
    latch.await();
}

}
