package com.example.transactionaltestintegration.service.runtimeexeption;

import com.example.transactionaltestintegration.entity.Post;
import com.example.transactionaltestintegration.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(propagation = Propagation.REQUIRES_NEW)
@RequiredArgsConstructor
public class NewTransactionalInnerService {

    private final PostRepository postRepository;
    private static final Logger log = LoggerFactory.getLogger(NonTransactionalInnerService.class);

    public void innerMethodThrowingRuntimeEx() {
        postRepository.save(new Post(2L, "[New Post]"));
        throw new RuntimeException("RuntimeException inside");
    }

    public void innerMethodCatchingRuntimeEx() {
        postRepository.save(new Post(2L, "[New Post]"));
        try {
            throw new RuntimeException("exception after save");
        } catch (RuntimeException ex) {
            log.warn("caught exception inside. e:{}", ex.getMessage());
        }
    }
}
