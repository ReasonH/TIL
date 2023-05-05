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
    @DisplayName("서비스 트랜잭션 O")
    void test() {
        cacheService.increment(testKey, false);
        Integer result = redisTemplate.opsForValue().get(testKey);
        assertThat(result).isEqualTo(1);
    }

    @Test
    @DisplayName("서비스 트랜잭션 O, 예외 발생")
    void test2() {
        Assertions.assertThatThrownBy(() -> cacheService.increment(testKey, true)).isInstanceOf(Exception.class);
        Integer result = redisTemplate.opsForValue().get(testKey);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("서비스 트랜잭션 O, 레디스 트랜잭션 내에서 값 가져오기" +
            "레디스 트랜잭션 실행 중 값을 조회할 수 없다.")
    void test3() {
        Integer result = cacheService.incrementAndGet(testKey);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("서비스 트랜잭션 X, 레디스 트랜잭션 내에서 값 가져오기" +
            "@Transactional이 없는 경우 레디스 트랜잭션은 동작하지 않는다.")
    void test4() {
        Integer result = cacheService.incrementAndGetNonTx(testKey);
        assertThat(result).isEqualTo(1);
    }

    @Test
    @DisplayName("redisTemplate 복합 사용 케이스" +
            "1. tx support 켜진 redisTemplate으로 값 증가" +
            "2. tx support 안켜진 redisTemplate으로 값 증가 및 조회")
    void test5() {
        Integer result = cacheService.incrementAndGetComplex(testKey);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("redisTemplate 복합 사용 케이스" +
            "1. tx support 켜진 redisTemplate으로 값 증가" +
            "2. tx support 안켜진, 다른 connection factory 사용하는 redisTemplate으로 값 증가 및 조회")
    void test6() {
        Integer result = cacheService.incrementAndGetComplexOtherCp(testKey);
        assertThat(result).isEqualTo(1);
    }
}