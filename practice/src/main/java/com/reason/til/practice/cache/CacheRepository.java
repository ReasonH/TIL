package com.reason.til.practice.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CacheRepository {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisTemplate<String, Object> redisTemplateTransactional;
    private final RedisTemplate<String, Object> redisTemplateOtherCp;

    public Long incrementAndGet(String key) {
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        return valueOperations.increment(key, 1);
    }

    public Long incrementAndGetIndependentCp(String key) {
        ValueOperations<String, Object> valueOperations = redisTemplateOtherCp.opsForValue();
        return valueOperations.increment(key, 1);
    }

    public Long incrementAndGetTransactional(String key) {
        ValueOperations<String, Object> valueOperations = redisTemplateTransactional.opsForValue();
        return valueOperations.increment(key, 1);
    }

    public void deleteCache(String key) {
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        redisTemplate.delete(key);
    }
}
