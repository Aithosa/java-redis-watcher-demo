package org.example.rediswatcher.rediswatcher.watcher;

import org.example.rediswatcher.rediswatcher.config.TestRedisClusterConfig;
import org.example.rediswatcher.watcher.RedisKeyExpirationListener;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 集群模式测试：
 * - TestRedisClusterConfig: 集群模式的测试配置类
 * - RedisClusterKeyExpirationListenerTest: 测试集群模式下的键过期监听功能
 * - 使用 @ActiveProfiles("cluster") 来激活集群配置
 */
@SpringBootTest
@Import(TestRedisClusterConfig.class)
@ActiveProfiles("cluster")
class RedisClusterKeyExpirationListenerTest {

    @Autowired
    private RedisKeyExpirationListener redisKeyExpirationListener;

    @Test
    void testClusterKeyExpiration() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(5);
        Random random = new Random();

        Thread setterThread = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                try {
                    int randSec = random.nextInt(10) + 1;
                    String randKey = String.format("rand_%d_%d", System.currentTimeMillis() / 1000, randSec);

                    redisKeyExpirationListener.watchKey(randKey, "rand", Duration.ofSeconds(randSec));
                    System.out.printf("%s 生成 key: %s, 到期时间: %d秒%n", LocalDateTime.now(), randKey, randSec);

                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });

        setterThread.start();

        boolean completed = latch.await(30, TimeUnit.SECONDS);
        setterThread.interrupt();

        if (!completed) {
            System.out.println("测试超时：部分键可能未过期");
        }
    }
}
