package com.example.transactionaltestintegration.service.transactionalevent.handler.event.update;

import com.example.transactionaltestintegration.entity.Post;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UpdateByIdTxListener {
    private String content;
    private Post post;
}
