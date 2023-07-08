package com.example.redisdemo.service;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class CacheServiceTest {

    @Autowired
    private CacheService cacheService;
    @Autowired
    private RedisTemplate<String, Integer> redisTemplate;

    String testKey = "TEST";

    @AfterEach
    public void tearDown() {
        redisTemplate.delete(testKey);
    }

    @Test
    @DisplayName("@Transactional 서비스, 예외 미발생")
    void test() {
        cacheService.increment(testKey, false);
        Integer result = redisTemplate.opsForValue().get(testKey);
        assertThat(result).isEqualTo(1);
    }

    @Test
    @DisplayName("@Transactional 서비스, 예외 발생")
    void test2() {
        Assertions.assertThatThrownBy(() -> cacheService.increment(testKey, true)).isInstanceOf(Exception.class);
        Integer result = redisTemplate.opsForValue().get(testKey);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("@Transactional 서비스 내에서 Tx support on인 redisTemplate으로 값 조회")
    void test3() {
        Integer result = cacheService.incrementAndGet(testKey);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("일반 서비스 내에서 Tx support on인 redisTemplate으로 값 조회")
    void test4() {
        Integer result = cacheService.incrementAndGetNonTx(testKey);
        assertThat(result).isEqualTo(1);
    }

    @Test
    @DisplayName("redisTemplate 복합 사용 케이스" +
            "1. tx support on인 redisTemplate으로 값 증가" +
            "2. tx support off인 redisTemplate으로 값 증가 및 조회")
    void test5() {
        Integer result = cacheService.incrementAndGetComplex(testKey);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("redisTemplate 복합 사용 케이스" +
            "1. tx support on인 redisTemplate으로 값 증가" +
            "2. tx support off + 다른 connection factory 사용하는 redisTemplate으로 값 증가 및 조회")
    void test6() {
        Integer result = cacheService.incrementAndGetComplexOtherCp(testKey);
        assertThat(result).isEqualTo(1);
    }
}