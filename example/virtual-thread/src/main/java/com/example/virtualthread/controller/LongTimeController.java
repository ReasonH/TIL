package com.example.virtualthread.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class LongTimeController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/")
    public String getThreadName() {
        return Thread.currentThread().toString();
    }

    @GetMapping("/block")
    public String getBlockedResponse() throws InterruptedException {
        Thread.sleep(1000);
        return "OK";
    }

    @GetMapping("/query")
    public String queryAndReturn() {
        // 쿼리 질의가 1초 걸린다고 가정
        return jdbcTemplate.queryForList("select sleep(0.3);").toString();
    }
}
