package com.reason.til.practice.service;

import com.reason.til.practice.cache.CacheRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class CacheTestService {

    private final CacheRepository cacheRepository;

    @Transactional
    public Long nonTransactionalCacheCall(String key) {
        Long result = cacheRepository.incrementAndGet(key);
        return result;
    }

    @Transactional
    public Long transactionalCacheCall(String key) {
        Long result = cacheRepository.incrementAndGetTransactional(key);
        return result;
    }

    @Transactional
    public Long complexCacheCall(String key) {
        cacheRepository.incrementAndGetTransactional(key);
        return cacheRepository.incrementAndGet(key);
    }

    @Transactional
    public void cacheEvict(String key) {
        cacheRepository.deleteCache(key);
    }
}
