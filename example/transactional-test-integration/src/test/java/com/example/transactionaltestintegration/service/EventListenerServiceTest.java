package com.example.transactionaltestintegration.service;

import com.example.transactionaltestintegration.entity.Post;
import com.example.transactionaltestintegration.entity.User;
import com.example.transactionaltestintegration.repository.PostRepository;
import com.example.transactionaltestintegration.repository.UserRepository;
import com.example.transactionaltestintegration.service.transactionalevent.EventListenerService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EventListenerServiceTest {

    @Autowired
    private EventListenerService eventListenerService;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        User user = userRepository.save(new User(1L, "[User]"));
        Post post = new Post(1L, "[Post]", "[Content]", user);
        postRepository.save(post);
    }

    @AfterEach
    void clear() {
        postRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("핸들러에서 전달받은 엔티티를 수정해도 DB에 반영되지 않는다.")
    @Order(1)
    public void updateByTxListenerNonTx() {
        Post post = eventListenerService.updateByTxListener(1L);
        assertThat(postRepository.findById(post.getId()).get().getContent()).isEqualTo("[Content]");
    }

    @Test
    @DisplayName("만약 엔티티를 수정하려면 REQUIRES_NEW로 새 트랜잭션을 만들고, 엔티티를 다시 조회해야 한다.")
    @Order(2)
    public void updateByTxListenerTxWithId() {
        Post post = eventListenerService.updateByIdTxListener(1L);
        assertThat(postRepository.findById(post.getId()).get().getContent())
                .isEqualTo("[Event Service Content]");
    }

    @Order(3)
    @DisplayName("리스너에서는 지연 로딩이 가능")
    @Test
    public void test1() {
        eventListenerService.lazyLoadingTxEvent(1L);
    }

    @Order(4)
    @DisplayName("리스너에서는 쓰기 작업이 불가능")
    @Test
    public void test2() {
        Post post = eventListenerService.txLazyLoadingTxEvent(1L);
        Long userId = post.getUser().getId();
        Assertions.assertThat(userRepository.findById(userId).get().getName()).isEqualTo("[User]");
    }

    @Order(5)
    @DisplayName("리스너에서는 쓰기 작업을 위해서는 REQUIRES_NEW 필요")
    @Test
    public void test3() {
        Post post = eventListenerService.txLazyLoadingNewTxEvent(1L);
        Long userId = post.getUser().getId();
        Assertions.assertThat(userRepository.findById(userId).get().getName()).isEqualTo("[Changed User]");
    }

    @Order(6)
    @DisplayName("비동기 리스너에서는 지연 로딩이 불가능")
    @Test
    public void test4() {
        eventListenerService.asyncTxLazyLoadingTxEvent(1L);
        try {
            Thread.sleep(1000); // 비동기 리스너 동작 대기
        } catch (Exception ignored) {
        }
    }
}