package org.example.rediswatcher.watcher;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 实现了 Redis 键过期监听功能
 * `KeyExpirationEventMessageListener`：这是一个监听到期事件的监听器。
 * 当 Redis 键过期时，Redis 会发布一个过期事件，`KeyExpirationEventMessageListener` 订阅这些事件进行处理
 * <p>
 * 主要特性：
 * - 自动监听 Redis 键过期事件
 * - 支持延迟队列功能
 * - 包含补偿机制，每10秒检查一次可能遗漏的过期键
 * - 使用 Spring Boot 的调度功能代替了原始项目的自定义任务池
 */
@Slf4j
@Component
public class RedisKeyExpirationListener extends KeyExpirationEventMessageListener {
    private final RedisTemplate<String, Object> redisTemplate;
    // 用于表示 Redis 有序集合（Sorted Set）的键名，该集合用于实现延迟队列功能
    private final String queueKey;
    private static final String DEFAULT_QUEUE_KEY = "job:delayed";

    public RedisKeyExpirationListener(RedisMessageListenerContainer listenerContainer,
                                      RedisTemplate<String, Object> redisTemplate) {
        super(listenerContainer);
        this.redisTemplate = redisTemplate;
        this.queueKey = DEFAULT_QUEUE_KEY;
    }

    /**
     * 当 Redis 触发键过期事件后，该方法会被调用
     *
     * @param message message must not be {@literal null}.
     * @param pattern pattern matching the channel (if specified) - can be {@literal null}.
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = message.toString();
        log.info("Key expired: {}", expiredKey);
        handleExpiredKey(expiredKey);
    }

    /**
     * 用于从 Redis 的 Sorted Set 移除过期的键
     *
     * @param key
     */
    private void handleExpiredKey(String key) {
        redisTemplate.opsForZSet().remove(queueKey, key);
    }

    /**
     * 补偿任务
     * 查找在特定时间范围内应过期但可能遗漏的键，并利用 `handleExpiredKey` 处理它们
     */
    public void makeUpTask() {
        Instant now = Instant.now();
        Set<Object> expiredKeys = redisTemplate.opsForZSet()
                .rangeByScore(queueKey, 0, now.toEpochMilli());

        if (expiredKeys != null && !expiredKeys.isEmpty()) {
            Set<String> stringKeys = expiredKeys.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .collect(Collectors.toSet());

            for (String key : stringKeys) {
                // 检查有序集合中的所有键，确认这些键是否已在 Redis 中实际过期并删除
                if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {
                    handleExpiredKey(key);
                }
            }
        }
    }

    /**
     * 对于需要监听的数据，通过这个方法写入
     * 会同时记录到zset中
     *
     * @param key
     * @param value
     * @param expiration
     */
    public void watchKey(String key, Object value, Duration expiration) {
        redisTemplate.opsForValue().set(key, value, expiration);
        if (!expiration.isZero()) {
            // TODO 为什么是这种计时方式
            Instant expirationTime = Instant.now().plus(expiration);
            redisTemplate.opsForZSet().add(queueKey, key, expirationTime.toEpochMilli());
        }
    }
}
