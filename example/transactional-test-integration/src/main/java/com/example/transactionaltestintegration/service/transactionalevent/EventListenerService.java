package com.example.transactionaltestintegration.service.transactionalevent;

import com.example.transactionaltestintegration.entity.Post;
import com.example.transactionaltestintegration.service.transactionalevent.handler.event.lazyloading.LazyAsyncTxListenerTx;
import com.example.transactionaltestintegration.service.transactionalevent.handler.event.lazyloading.LazyTxListener;
import com.example.transactionaltestintegration.service.transactionalevent.handler.event.lazyloading.LazyTxListenerNewTx;
import com.example.transactionaltestintegration.service.transactionalevent.handler.event.lazyloading.LazyTxListenerTx;
import com.example.transactionaltestintegration.service.transactionalevent.handler.event.update.UpdateByIdTxListener;
import com.example.transactionaltestintegration.service.transactionalevent.handler.event.update.UpdateTxListener;
import com.example.transactionaltestintegration.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EventListenerService {

    private final PostRepository postRepository;
    private final ApplicationEventPublisher eventPublisher;

    public void lazyLoadingTxEvent(long id) {
        Post post = postRepository.findById(id).get();
        eventPublisher.publishEvent(new LazyTxListener(post));
    }

    @Transactional
    public Post txLazyLoadingTxEvent(long id) {
        Post post = postRepository.findById(id).get();
        eventPublisher.publishEvent(new LazyTxListenerTx(post));
        return post;
    }

    @Transactional
    public Post txLazyLoadingNewTxEvent(long id) {
        Post post = postRepository.findById(id).get();
        eventPublisher.publishEvent(new LazyTxListenerNewTx(post));
        return post;
    }

    @Transactional
    public void asyncTxLazyLoadingTxEvent(long id) {
        Post post = postRepository.findById(id).get();
        eventPublisher.publishEvent(new LazyAsyncTxListenerTx(post));
    }

    @Transactional
    public Post updateByTxListener(long id) {
        Post post = postRepository.findById(id).get();
        eventPublisher.publishEvent(new UpdateTxListener("[Event Service Content]", post));
        return post;
    }

    @Transactional
    public Post updateByIdTxListener(long id) {
        Post post = postRepository.findById(id).get();
        eventPublisher.publishEvent(new UpdateByIdTxListener("[Event Service Content]", post));
        return post;
    }
}
