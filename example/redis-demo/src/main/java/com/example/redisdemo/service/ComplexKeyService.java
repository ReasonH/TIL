package com.example.redisdemo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ComplexKeyService {

    @Qualifier("redisTemplateTransactional")
    private final RedisTemplate<String, Integer> redisTemplate;

    @Transactional
    public void multiKeyOperation(String key1, String key2) {
        redisTemplate.opsForValue().increment(key1);
        redisTemplate.opsForValue().increment(key2);
    }
}
