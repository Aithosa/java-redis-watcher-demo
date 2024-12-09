package org.example.rediswatcher.rediswatcher.watcher;

import org.example.rediswatcher.watcher.RedisKeyExpirationListener;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.Topic;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 单机模式测试 Redis 的键过期监听功能：
 * - RedisKeyExpirationListenerTest: 测试单机模式下的键过期监听功能
 * - 使用 CountDownLatch 来控制测试的执行时间
 * - 模拟随机生成带过期时间的键
 */
@SpringBootTest
class RedisKeyExpirationListenerTest {
    @Autowired
    private RedisKeyExpirationListener redisKeyExpirationListener;

    @Autowired
    private RedisMessageListenerContainer messageListenerContainer;

    @Test
    void testKeyExpiration() throws InterruptedException {
        // 创建一个 CountDownLatch 来等待测试完成
        CountDownLatch latch = new CountDownLatch(5);
        Random random = new Random();

        MessageListener testListener = new MessageListener() {
            @Override
            public void onMessage(Message message, byte[] pattern) {
                System.out.printf("%s 键已过期: %s%n", LocalDateTime.now(), message.toString());
                latch.countDown(); // 每当有键过期时，计数器减一
            }
        };

        // 获取一个能够以编程方式注册监听器的适当工具
        Topic topic = new PatternTopic("__keyevent@*__:expired");

        // 临时注册监听器
        messageListenerContainer.addMessageListener(testListener, topic);

        try {
            // 创建并启动一个线程来设置带有随机过期时间的键
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

        } finally {
            // 确保在测试后移除监听器
            messageListenerContainer.removeMessageListener(testListener, topic);
        }
    }
}