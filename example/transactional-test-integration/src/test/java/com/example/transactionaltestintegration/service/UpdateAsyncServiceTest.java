package com.example.transactionaltestintegration.service;

import com.example.transactionaltestintegration.entity.Comment;
import com.example.transactionaltestintegration.repository.CommentRepository;
import com.example.transactionaltestintegration.service.updateasync.UpdateAsyncService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UpdateAsyncServiceTest {

    @Autowired
    private UpdateAsyncService updateAsyncService;
    @Autowired
    private CommentRepository commentRepository;

    @BeforeEach
    void setUp() {
        Comment comment = commentRepository.save(new Comment(1L, "[Comment]"));
    }

    @AfterEach
    void clear() {
        commentRepository.deleteAllInBatch();
    }

    @Test
    @Order(1)
    @DisplayName("스레드에서는 영속성 객체를 수정할 때, 메인 스레드가 먼저 종료되면 dirty checking은 동작하지 않는다.")
    public void updateWithFunctionInThread() {
        updateAsyncService.updateAsync(1L);
        assertThat(commentRepository.findById(1L).get().getContent()).isEqualTo("[Comment]");
    }

    @Test
    @Order(3)
    @DisplayName("메인 스레드에서 비동기 호출 종료를 대기하는 경우 dirty checking이 동작한다.")
    public void updateWithFunctionInThreadAndWait() {
        updateAsyncService.updateAsyncAndWait(1L);
        assertThat(commentRepository.findById(1L).get().getContent()).isEqualTo("[New Comment]");
    }
}