package com.example.redisdemo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class CacheService {

    private final RedisTemplate<String, Integer> redisTemplate;
    @Qualifier("redisTemplateTransactional")
    private final RedisTemplate<String, Integer> redisTemplateTransactional;
    @Qualifier("redisTemplateOtherCp")
    private final RedisTemplate<String, Integer> redisTemplateOtherCp;

    @Transactional
    public void increment(String key, boolean exception) {
        ValueOperations<String, Integer> valueOperations = redisTemplateTransactional.opsForValue();
        if (exception) {
            throw new RuntimeException();
        }
        valueOperations.increment(key);
    }

    @Transactional
    public Integer incrementAndGet(String key) {
        ValueOperations<String, Integer> valueOperations = redisTemplateTransactional.opsForValue();
        valueOperations.increment(key);
        return valueOperations.get(key);
    }

    public Integer incrementAndGetNonTx(String key) {
        ValueOperations<String, Integer> valueOperations = redisTemplateTransactional.opsForValue();
        valueOperations.increment(key);
        return valueOperations.get(key);
    }

    @Transactional
    public Integer incrementAndGetComplex(String key) {
        ValueOperations<String, Integer> valueOperation1 = redisTemplateTransactional.opsForValue();
        valueOperation1.increment(key);

        ValueOperations<String, Integer> valueOperation2 = redisTemplate.opsForValue();
        valueOperation2.increment(key);
        return valueOperation2.get(key);
    }

    @Transactional
    public Integer incrementAndGetComplexOtherCp(String key) {
        ValueOperations<String, Integer> valueOperation1 = redisTemplateTransactional.opsForValue();
        valueOperation1.increment(key);

        ValueOperations<String, Integer> valueOperation2 = redisTemplateOtherCp.opsForValue();
        valueOperation2.increment(key);
        return valueOperation2.get(key);
    }
}
