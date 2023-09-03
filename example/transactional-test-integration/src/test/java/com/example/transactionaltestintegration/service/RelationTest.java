package com.example.transactionaltestintegration.service;

import com.example.transactionaltestintegration.entity.Category;
import com.example.transactionaltestintegration.entity.Comment;
import com.example.transactionaltestintegration.entity.Post;
import com.example.transactionaltestintegration.entity.User;
import com.example.transactionaltestintegration.repository.CategoryRepository;
import com.example.transactionaltestintegration.repository.CommentRepository;
import com.example.transactionaltestintegration.repository.PostRepository;
import com.example.transactionaltestintegration.repository.UserRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RelationTest {

    @Autowired
    private PostRepository postRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CommentRepository commentRepository;

    @BeforeEach
    void setUp() {
        User user = userRepository.save(new User(1L,"[User]"));
        Category category = categoryRepository.save(new Category(1L, "[Category]"));
        Post post = postRepository.save(new Post(1L, "[Post]", "[Content]", user, category));
        Comment comment = commentRepository.save(new Comment(1L, "[Comment]", "Lee", post));
    }

    @AfterEach
    void clear() {
        commentRepository.deleteAllInBatch();
        postRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("JPQL로 Comment를 조회하면, Post와 Category를 조회하는 추가 쿼리가 발생한다.")
    public void test1() {
        Comment result = commentRepository.findByIdQuery(1L).get();
        Assertions.assertThat(result.getPost().getCategory().getTitle()).isEqualTo("[Category]");
    }

    @Test
    @DisplayName("JPQL로 Comment와 Post를 fetch 하면, Category를 조회하는 추가 쿼리가 발생한다.")
    public void test2() {
        Comment result = commentRepository.findByIdFetchQuery(1L).get();
        Assertions.assertThat(result.getPost().getCategory().getTitle()).isEqualTo("[Category]");
    }

    @Test
    @DisplayName("함수형 쿼리로 Comment를 조회하면, Post와 Category까지 한 번에 left outer join하는 쿼리가 발생한다.")
    public void test3() {
        Comment result = commentRepository.findById(1L).get();
        Assertions.assertThat(result.getPost().getCategory().getTitle()).isEqualTo("[Category]");
    }
}