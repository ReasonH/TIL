package com.example.transactionaltestintegration.service.transactionalevent.handler.event.lazyloading;

import com.example.transactionaltestintegration.entity.Post;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LazyTxListener {
    Post post;
}
