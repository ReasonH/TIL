package com.example.transactionaltestintegration.service;

import com.example.transactionaltestintegration.entity.Post;
import com.example.transactionaltestintegration.repository.PostRepository;
import com.example.transactionaltestintegration.service.isolation.IsolationService;
import com.sun.tools.javac.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class IsolationTest {

    @Autowired
    private IsolationService isolationService;
    @Autowired
    private PostRepository postRepository;

    @BeforeEach
    void setUp() {
        postRepository.save(new Post(1L, "[Title]"));
    }

    @AfterEach
    void clear() {
        postRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("READ COMMITTED여도 JPQL 조회를 두 번 하면 항상 결과가 같다.")
    public void test1() throws ExecutionException, InterruptedException {
        CompletableFuture<List<String>> titleList = CompletableFuture.supplyAsync(
                () -> isolationService.getTitleReadCommitted(1L)
        );
        try {
            Thread.sleep(1000);
        } catch (Exception ignored) {

        }
        postRepository.save(new Post(1L, "[New Title]"));
        List<String> titles = titleList.get();
        assertThat(titles.get(0)).isEqualTo(titles.get(1));
    }

    @Test
    @DisplayName("READ COMMITTED에서 Projection을 사용하는 경우 다른 결과를 얻을 수 있다.")
    public void test2() throws ExecutionException, InterruptedException {
        CompletableFuture<List<String>> titleList = CompletableFuture.supplyAsync(
                () -> isolationService.getTitleByProjectionReadCommitted(1L)
        );
        try {
            Thread.sleep(1000);
        } catch (Exception ignored) {

        }
        postRepository.save(new Post(1L, "[New Title]"));
        List<String> titles = titleList.get();
        assertThat(titles.get(0)).isNotEqualTo(titles.get(1));
    }

    @Test
    @DisplayName("REPEATABLE READ인 경우 Projection에서도 동일한 결과를 얻는다.")
    public void test3() throws ExecutionException, InterruptedException {
        CompletableFuture<List<String>> titleList = CompletableFuture.supplyAsync(
                () -> isolationService.getTitleByProjectionRepeatableRead(1L)
        );
        try {
            Thread.sleep(1000);
        } catch (Exception ignored) {

        }
        postRepository.save(new Post(1L, "[New Title]"));
        List<String> titles = titleList.get();
        assertThat(titles.get(0)).isEqualTo(titles.get(1));
    }
}
