package com.example.transactionaltestintegration.service.runtimeexeption;

import com.example.transactionaltestintegration.entity.Post;
import com.example.transactionaltestintegration.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RuntimeExceptionService {
    private final NonTransactionalInnerService nonTransactionalInnerService;
    private final TransactionalInnerService transactionalInnerService;
    private final NewTransactionalInnerService newTransactionalInnerService;
    private final PostRepository postRepository;

    private static final Logger log = LoggerFactory.getLogger(RuntimeExceptionService.class);

    @Transactional
    public void nonTransactionalThrowingRuntimeEx() {
        try {
            postRepository.save(new Post(1L, "[Post]"));
            nonTransactionalInnerService.innerMethodThrowingRuntimeEx();
        } catch (RuntimeException ex) {
            log.warn("OuterService caught exception at outer. ex:{}", ex.getMessage());
        }
    }

    @Transactional
    public void nonTransactionalCatchingRuntimeEx() {
        try {
            postRepository.save(new Post(1L, "[Post]"));
            nonTransactionalInnerService.innerMethodCatchingRuntimeEx();
        } catch (RuntimeException ex) {
            log.warn("OuterService caught exception at outer. ex:{}", ex.getMessage());
        }
    }

    @Transactional
    public void transactionalThrowingRuntimeEx() {
        try {
            postRepository.save(new Post(1L, "[Post]"));
            transactionalInnerService.innerMethodThrowingRuntimeEx();
        } catch (RuntimeException ex) {
            log.warn("OuterService caught exception at outer. ex:{}", ex.getMessage());
        }
    }

    @Transactional
    public void transactionalCatchingRuntimeEx() {
        try {
            postRepository.save(new Post(1L, "[Post]"));
            transactionalInnerService.innerMethodCatchingRuntimeEx();
        } catch (RuntimeException ex) {
            log.warn("OuterService caught exception at outer. ex:{}", ex.getMessage());
        }
    }

    @Transactional
    public void newTransactionalThrowingRuntimeEx() {
        try {
            postRepository.save(new Post(1L, "[Post]"));
            newTransactionalInnerService.innerMethodThrowingRuntimeEx();
        } catch (RuntimeException ex) {
            log.warn("OuterService caught exception at outer. ex:{}", ex.getMessage());
        }
    }

    @Transactional
    public void newTransactionalCatchingRuntimeEx() {
        try {
            postRepository.save(new Post(1L, "[Post]"));
            newTransactionalInnerService.innerMethodCatchingRuntimeEx();
        } catch (RuntimeException ex) {
            log.warn("OuterService caught exception at outer. ex:{}", ex.getMessage());
        }
    }
}
