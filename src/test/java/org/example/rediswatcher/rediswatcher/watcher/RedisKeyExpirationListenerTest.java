package org.example.rediswatcher.rediswatcher.watcher;

import org.example.rediswatcher.watcher.RedisKeyExpirationListener;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 单机模式测试：
 * - RedisKeyExpirationListenerTest: 测试单机模式下的键过期监听功能
 * - 使用 CountDownLatch 来控制测试的执行时间
 * - 模拟随机生成带过期时间的键
 */
@SpringBootTest
class RedisKeyExpirationListenerTest {

    @Autowired
    private RedisKeyExpirationListener redisKeyExpirationListener;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    void testKeyExpiration() throws InterruptedException {
        // 创建一个 CountDownLatch 来等待测试完成
        CountDownLatch latch = new CountDownLatch(5);
        Random random = new Random();

        // 创建并启动一个线程来设置随机过期的键
        Thread setterThread = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                try {
                    int randSec = random.nextInt(10) + 1;
                    String randKey = String.format("rand_%d_%d", System.currentTimeMillis() / 1000, randSec);

                    redisKeyExpirationListener.watchKey(randKey, "rand", Duration.ofSeconds(randSec));
                    System.out.printf("%s 生成 key: %s, 到期时间: %d秒%n", LocalDateTime.now(), randKey, randSec);

                    Thread.sleep(1000); // 每秒生成一个键
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });

        setterThread.start();

        // 等待最多30秒，确保所有键都有机会过期
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        setterThread.interrupt();

        if (!completed) {
            System.out.println("测试超时：部分键可能未过期");
        }
    }
}
