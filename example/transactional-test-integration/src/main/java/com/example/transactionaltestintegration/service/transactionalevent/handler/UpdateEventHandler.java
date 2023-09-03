package com.example.transactionaltestintegration.service.transactionalevent.handler;

import com.example.transactionaltestintegration.service.transactionalevent.handler.event.update.UpdateByIdTxListener;
import com.example.transactionaltestintegration.service.transactionalevent.handler.event.update.UpdateTxListener;
import com.example.transactionaltestintegration.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class UpdateEventHandler {

    private final PostRepository postRepository;

    @TransactionalEventListener
    @Transactional
    public void onMyEvent(UpdateTxListener event) {
        event.getPost().setContent(event.getContent());
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onMyEvent(UpdateByIdTxListener event) {
        postRepository.findById(event.getPost().getId()).get().setContent(event.getContent());
    }
}
