package com.example.transactionaltestintegration.repository;

import com.example.transactionaltestintegration.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    Post findByTitle(String title);

    @Query(" SELECT p.title " +
            "  FROM Post p " +
            " WHERE p.id = ?1 ")
    String findTitleByProjection(Long id);
}
