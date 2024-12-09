package org.example.rediswatcher.scheduler;

import org.example.rediswatcher.watcher.RedisKeyExpirationListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 实现了补偿任务的定时执行
 */
@Component
public class MakeUpTaskScheduler {

    private final RedisKeyExpirationListener redisKeyExpirationListener;

    public MakeUpTaskScheduler(RedisKeyExpirationListener redisKeyExpirationListener) {
        this.redisKeyExpirationListener = redisKeyExpirationListener;
    }

    /**
     * 每10秒执行一次补偿任务
     * 查找在特定时间范围内应过期但可能遗漏的键，并利用 `handleExpiredKey` 处理它们
     */
    @Scheduled(fixedDelay = 10000)
    public void scheduleMakeUpTask() {
        redisKeyExpirationListener.makeUpTask();
    }
}
