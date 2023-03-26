package com.reason.til.practice.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("local")
@SpringBootTest
class CacheTestServiceTest {

    @Autowired
    private CacheTestService cacheTestService;
    String test = "TEST";

    @AfterEach
    public void tearDown() {
        cacheTestService.cacheEvict(test);
    }

    @Test
    public void test() {
        Long aLong = cacheTestService.nonTransactionalCacheCall(test);
        assertThat(aLong).isEqualTo(1L);
    }

    @Test
    public void test2() {
        Long aLong = cacheTestService.transactionalCacheCall(test);
        assertThat(aLong).isNull();
    }

    @Test
    @DisplayName("한 트랜잭션 안에서 트랜잭션 " +
            "1. 트랜잭션 서포트 있는 redisTemplate" +
            "2. 트랜잭션 서포트 없는 redisTemplate 순차 호출" +
            "3. 두 redisTemplate connectionFactory 동일")
    public void test3() {
        Long aLong = cacheTestService.complexCacheCall(test);
        assertThat(aLong).isNull();
    }

    @Test
    @DisplayName("한 트랜잭션 안에서 " +
            "1. 트랜잭션 서포트 있는 restTemplate" +
            "2. 트랜잭션 서포트 없는 redisTemplate 순차 호출" +
            "3. 두 redisTemplate connectionFactory 다름")
    public void test3() {
        Long aLong = cacheTestService.complexCacheCall(test);
        assertThat(aLong).isNull();
    }
}