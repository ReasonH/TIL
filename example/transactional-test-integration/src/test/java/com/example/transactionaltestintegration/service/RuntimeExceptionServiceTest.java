package com.example.transactionaltestintegration.service;

import com.example.transactionaltestintegration.repository.PostRepository;
import com.example.transactionaltestintegration.service.runtimeexeption.RuntimeExceptionService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.UnexpectedRollbackException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RuntimeExceptionServiceTest {
    @Autowired
    private RuntimeExceptionService runtimeExceptionService;
    @Autowired
    private PostRepository postRepository;

    @AfterEach
    void clear() {
        postRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("런타임 예외를 던지는 @Transactional 메서드 호출")
    public void transactionalThrowingRuntimeEx() {

        Assertions.assertThatThrownBy(() -> runtimeExceptionService.transactionalThrowingRuntimeEx())
                .isInstanceOf(UnexpectedRollbackException.class);
    }

    @Test
    @DisplayName("런타임 예외를 내부에서 처리하는 @Transactional 메서드 호출")
    public void transactionalCatchingRuntimeEx() {
        runtimeExceptionService.transactionalCatchingRuntimeEx();
        assertThat(postRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("런타임 예외를 던지는 일반 메서드 호출")
    public void nonTransactionalThrowingRuntimeEx() {
        runtimeExceptionService.nonTransactionalThrowingRuntimeEx();
        assertThat(postRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("런타임 예외를 내부에서 처리하는 일반 메서드 호출")
    public void nonTransactionalCatchingRuntimeEx() {
        runtimeExceptionService.nonTransactionalCatchingRuntimeEx();
        assertThat(postRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("런타임 예외를 던지는 @Transactional(REQUIRES_NEW) 메서드 호출")
    public void newTransactionalThrowingRuntimeEx() {
        runtimeExceptionService.newTransactionalThrowingRuntimeEx();
        assertThat(postRepository.count()).isEqualTo(1);
        assertThat(postRepository.findAll().get(0).getTitle()).isEqualTo("[Post]");
    }

    @Test
    @DisplayName("런타임 예외를 내부에서 처리하는 @Transactional(REQUIRES_NEW) 메서드 호출")
    public void newTransactionalCatchingRuntimeEx() {
        runtimeExceptionService.newTransactionalCatchingRuntimeEx();
        assertThat(postRepository.count()).isEqualTo(2);
    }
}
