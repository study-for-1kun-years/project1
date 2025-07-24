package com.example.demo3.config.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class RedisSessionRegistry {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private final String SESSION_KEY_PREFIX = "spring:session:sessions:";

    public List<String> listActiveSessions() {
        Set<String> keys = redisTemplate.keys(SESSION_KEY_PREFIX + "*");
        if (keys == null) return List.of();
        return keys.stream().map(k -> k.replace(SESSION_KEY_PREFIX, "")).toList();
    }

    public int getSessionCount() {
        Set<String> keys = redisTemplate.keys(SESSION_KEY_PREFIX + "*");
        return keys != null ? keys.size() : 0;
    }
}