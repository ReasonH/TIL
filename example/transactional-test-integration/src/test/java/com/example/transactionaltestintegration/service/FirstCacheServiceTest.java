package com.example.transactionaltestintegration.service;

import com.example.transactionaltestintegration.entity.Post;
import com.example.transactionaltestintegration.entity.User;
import com.example.transactionaltestintegration.repository.PostRepository;
import com.example.transactionaltestintegration.repository.UserRepository;
import com.example.transactionaltestintegration.service.firstcache.FirstCacheService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FirstCacheServiceTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private FirstCacheService firstCacheService;

    @AfterEach
    void clear() {
        postRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @Test
    @Transactional
    @DisplayName("1차 캐시에 값이 있으면 JPQL 조회 결과는 버려진다.")
    public void test1() {
        User user = new User(1L, "[User]");
        userRepository.save(user);

        Post post1 = new Post(1L, "[Post1]", "[Content1]", user);
        Post post2 = new Post(2L, "[Post2]", "[Content2]", user);
        postRepository.save(post1);
        postRepository.save(post2);

        List<User> userList = userRepository.findAllByJoinFetch();
        assertThat(userList.get(0).getPostList().size()).isEqualTo(0);
    }

    @Test
    @Transactional
    @DisplayName("JPQL 조회 시, 현재 1차 캐시 값을 flush 한다. 즉, Post는 변경된 값인 [Changed Post]를 통해 조회할 수 있다.")
    public void test2() {
        Post post = postRepository.save(new Post(1L, "[Post]", "[Content]", null));
        assertThat(post.getTitle()).isEqualTo("[Post]");
        post.setTitle("[Changed Post]");

        assertThat(postRepository.findByTitle("[Post]")).isNull();
        assertThat(postRepository.findByTitle("[Changed Post]")).isNotNull();
    }

    @Nested
    @DisplayName("객체 A의 유니크 값 a를 b로 수정, a를 이용한 새로운 객체 B를 만들어서 save")
    class FlushAndJPQL {

        @BeforeEach
        void setUp() {
            User user = userRepository.save(new User(1L, "[User]"));
            Post post = postRepository.save(new Post(1L, "[Post]", "[Content]", user));
        }

        @Test
        @DisplayName("기존 객체 수정, update 쿼리가 새로운 객체 insert 이후에 수행되므로 실패한다.")
        public void foo3() {
            assertThatThrownBy(() -> firstCacheService.updateUniqueColumn(1L))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("기존 객체 수정 후 save 호출, save가 update 취급되므로 동일하게 실패한다.")
        public void foo4() {
            assertThatThrownBy(() -> firstCacheService.updateAndSaveUniqueColumn(1L))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("기존 객체 수정 후 saveAndFlush 호출, update 쿼리가 미리 반영되고 성공한다.")
        public void foo5() {
            firstCacheService.updateAndSaveAndFlushUniqueColumn(1L);
            assertThat(postRepository.findAll().size()).isEqualTo(2);
        }
    }

}

