package com.example.transactionaltestintegration.service.firstcache;

import com.example.transactionaltestintegration.entity.Post;
import com.example.transactionaltestintegration.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FirstCacheService {

    private final PostRepository postRepository;

    @Transactional
    public void updateUniqueColumn(long id) {
        Post post = postRepository.findById(id).get();
        String oldContent = post.getContent();
        post.setContent("[New Content]");

        Post newPost = new Post(2L, "[New Post]", oldContent, null);
        postRepository.save(newPost);
    }

    @Transactional
    public void updateAndSaveUniqueColumn(long id) {
        Post post = postRepository.findById(id).get();
        String oldContent = post.getContent();
        post.setContent("[New Content]");
        postRepository.save(post);

        Post newPost = new Post(2L, "[New Post]", oldContent, null);
        postRepository.save(newPost);
    }

    @Transactional
    public void updateAndSaveAndFlushUniqueColumn(long id) {
        Post post = postRepository.findById(id).get();
        String oldContent = post.getContent();
        post.setContent("[New Content]");
        postRepository.saveAndFlush(post);

        Post newPost = new Post(2L, "[New Post]", oldContent, null);
        postRepository.save(newPost);
    }
}
