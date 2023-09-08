package com.example.transactionaltestintegration.service.isolation;

import com.example.transactionaltestintegration.repository.PostRepository;
import com.sun.tools.javac.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IsolationService {

    private final PostRepository postRepository;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<String> getTitleReadCommitted(Long id) {
        String oldTitle = postRepository.findById(id).get().getTitle();
        try {
            Thread.sleep(2000);
        } catch (Exception e) {

        }
        String newTitle = postRepository.findById(id).get().getTitle();
        return List.of(oldTitle, newTitle);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<String> getTitleByProjectionReadCommitted(Long id) {
        String oldTitle = postRepository.findById(id).get().getTitle();
        try {
            Thread.sleep(2000);
        } catch (Exception e) {

        }
        String newTitle = postRepository.findTitleByProjection(id);
        return List.of(oldTitle, newTitle);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public List<String> getTitleByProjectionRepeatableRead(Long id) {
        String oldTitle = postRepository.findById(id).get().getTitle();
        try {
            Thread.sleep(2000);
        } catch (Exception e) {

        }
        String newTitle = postRepository.findTitleByProjection(id);
        return List.of(oldTitle, newTitle);
    }
}
