package org.example.rediswatcher.rediswatcher.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.util.Arrays;

@TestConfiguration
public class TestRedisClusterConfig {

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisClusterConfiguration clusterConfig = new RedisClusterConfiguration(
                Arrays.asList(
                        "localhost:30001",
                        "localhost:30002",
                        "localhost:30003"
                )
        );
        return new LettuceConnectionFactory(clusterConfig);
    }
}
