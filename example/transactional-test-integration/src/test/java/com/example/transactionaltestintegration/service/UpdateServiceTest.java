package com.example.transactionaltestintegration.service;

import com.example.transactionaltestintegration.entity.Comment;
import com.example.transactionaltestintegration.repository.CommentRepository;
import com.example.transactionaltestintegration.service.update.UpdateService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UpdateServiceTest {
    @Autowired
    private UpdateService updateService;
    @Autowired
    private CommentRepository commentRepository;

    @BeforeEach
    void setUp() {
        commentRepository.save(new Comment(1L, "[Comment]"));
    }

    @AfterEach
    void clear() {
        commentRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("부모에서 조회 후 전달, 자식에서 전달받은 객체 수정, 부모에서 객체 수정하면, dirty-checking에 의해 최종 객체 상태가 DB에 반영된다.")
    @Order(1)
    public void updateByEntityAndUpdateOutside() {
        updateService.updateByEntityAndUpdateOutside(1L);
        Comment comment = commentRepository.findById(1L).get();
        assertThat(comment.getContent()).isEqualTo("[Comment]");
    }

    @Test
    @DisplayName("자식에서 객체 조회 및 수정 후 부모로 반환해서 재수정하면, dirty-checking 미동작, 내부 객체 상태가 DB에 반영된다.")
    @Order(2)
    public void updateByIdAndGetEntityAndUpdateOutside() {
        updateService.updateByIdAndGetEntityAndUpdateOutside(1L);
        Comment comment = commentRepository.findById(1L).get();
        assertThat(comment.getContent()).isEqualTo("[New Comment]");
    }
}
