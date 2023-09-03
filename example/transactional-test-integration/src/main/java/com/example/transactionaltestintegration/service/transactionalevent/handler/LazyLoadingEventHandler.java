package com.example.transactionaltestintegration.service.transactionalevent.handler;

import com.example.transactionaltestintegration.entity.User;
import com.example.transactionaltestintegration.service.transactionalevent.handler.event.lazyloading.LazyAsyncTxListenerTx;
import com.example.transactionaltestintegration.service.transactionalevent.handler.event.lazyloading.LazyTxListener;
import com.example.transactionaltestintegration.service.transactionalevent.handler.event.lazyloading.LazyTxListenerNewTx;
import com.example.transactionaltestintegration.service.transactionalevent.handler.event.lazyloading.LazyTxListenerTx;
import com.example.transactionaltestintegration.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class LazyLoadingEventHandler {
    private static final Logger log = LoggerFactory.getLogger(LazyLoadingEventHandler.class);

    private final UserRepository userRepository;

    @TransactionalEventListener
    public void onMyEvent(LazyTxListener event) {
        event.getPost().getUser().getName();
    }

    @TransactionalEventListener
    @Transactional
    public void onMyEvent(LazyTxListenerTx event) {
        User user = event.getPost().getUser();
        user.setName("[Changed User]");
        userRepository.save(user);
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onMyEvent(LazyTxListenerNewTx event) {
        User user = event.getPost().getUser();
        user.setName("[Changed User]");
        userRepository.save(user);
    }

    @TransactionalEventListener
    @Async
    @Transactional
    public void onMyEvent(LazyAsyncTxListenerTx event) {
        event.getPost().getUser().getName();
    }
}
