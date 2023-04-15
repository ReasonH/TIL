package com.example.delegationsecurity.test;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RequiredArgsConstructor
@Service
public class TestService {

    @Qualifier("userThreadPool")
    private final TaskExecutor taskExecutor;

    String nestedWaitThread(String securityPrincipal, CompletableFuture<String> securityPrincipalAfterAsync) {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken("key", securityPrincipal, Collections.singleton(getRole())));
        taskExecutor.execute(() -> {
            log.error("Thread ID: {}, async 2 inner start", Thread.currentThread().getId());
            securityPrincipalAfterAsync.complete(SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString());
        });
        return SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
    }

    String refreshSecurityContext(String securityPrincipal) {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken("key", securityPrincipal, Collections.singleton(getRole())));
        return SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
    }

    private SimpleGrantedAuthority getRole() {
        return new SimpleGrantedAuthority("role");
    }
}