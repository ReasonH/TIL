package com.example.transactionaltestintegration.service.transactionalevent.handler.event.update;

import com.example.transactionaltestintegration.entity.Post;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UpdateTxListener {
    private String content;
    private Post post;
}
