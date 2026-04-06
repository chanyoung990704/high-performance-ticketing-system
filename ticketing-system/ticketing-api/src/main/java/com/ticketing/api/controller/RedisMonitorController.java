package com.ticketing.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Properties;

@RestController
@RequestMapping("/api/v1/admin/redis")
@RequiredArgsConstructor
public class RedisMonitorController {

    private final RedisTemplate<String, String> redisTemplate;

    @GetMapping("/info")
    public Map<String, Object> getRedisInfo() {
        Properties info = redisTemplate.getConnectionFactory()
                .getConnection().serverCommands().info("memory");

        return Map.of(
                "used_memory_human", info.getProperty("used_memory_human") != null ? info.getProperty("used_memory_human") : "N/A",
                "maxmemory_human", info.getProperty("maxmemory_human") != null ? info.getProperty("maxmemory_human") : "unlimited",
                "mem_fragmentation_ratio", info.getProperty("mem_fragmentation_ratio") != null ? info.getProperty("mem_fragmentation_ratio") : "N/A"
        );
    }

    @GetMapping("/queue/{eventId}/size")
    public Map<String, Long> getQueueSize(@PathVariable Long eventId) {
        String key = String.format("queue:%d", eventId);
        Long size = redisTemplate.opsForZSet().size(key);
        return Map.of("queueSize", size != null ? size : 0L);
    }
}
