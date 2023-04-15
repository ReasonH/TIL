package com.example.delegationsecurity.test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.task.TaskExecutor;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class TestServiceTest {

    @Autowired
    @Qualifier("userThreadPool")
    TaskExecutor taskExecutor;

    @Autowired
    TestService testService;

    @Test
    @DisplayName("DelegatingSecurityContextRunnable 사용 시, 스레드 고갈 상황에서 발생하는 SecurityContext 갱신 문제 테스트")
    public void test() throws InterruptedException, ExecutionException {

        // 1. 특정 작업이 스레드를 점유
        longTakeTask();
        String firstPrincipal = "DUMMY";
        String secondPrincipal = "NEW_DUMMY";
        CompletableFuture<String> firstAsyncPrincipal = new CompletableFuture<>();

        // 2. API1 수행, SecurityContext 설정 및 taskQueue에 task 할당 후 종료
        assertThat(testService.nestedWaitThread(firstPrincipal, firstAsyncPrincipal), is(firstPrincipal));
        // 3. API2 수행, SecurityContext 설정 후 종료
        assertThat(testService.refreshSecurityContext(secondPrincipal), is(secondPrincipal));

        // 4. API1 내부 스레드에 전달된 SecurityContext는 API2에서 갱신된 정보이다.
        CountDownLatch latch = new CountDownLatch(1);
        latch.await(3, TimeUnit.SECONDS);
        assertThat(firstAsyncPrincipal.get(), is(secondPrincipal));
    }

    private void longTakeTask() {
        taskExecutor.execute(() -> {
            try {
                Thread.sleep(2000);
            } catch (Exception ignored) {
            }
        });
    }
}