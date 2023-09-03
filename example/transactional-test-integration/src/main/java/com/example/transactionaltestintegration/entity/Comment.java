package com.example.transactionaltestintegration.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Comment {
    @Id
    private Long id;

    @ManyToOne
    private Post post;

    private String content;

    private String author;

    public Comment(Long id, String content) {
        this.id = id;
        this.content = content;
    }

    public Comment(Long id, String content, String author, Post post) {
        this.id = id;
        this.content = content;
        this.author = author;
        this.post = post;
    }

    @Override
    public String toString() {
        return getId() + " " + getContent();
    }
}
