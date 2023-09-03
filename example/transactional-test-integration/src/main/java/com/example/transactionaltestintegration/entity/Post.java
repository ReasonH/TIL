package com.example.transactionaltestintegration.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Post {
    @Id
    private Long id;

    private String title;

    @Column(unique = true)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    private Category category;

    public Post(Long id, String title) {
        this.id = id;
        this.title = title;
    }

    public Post(Long id, String title, String content, User user) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.user = user;
    }

    public Post(Long id, String title, String content, User user, Category category) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.user = user;
        this.category = category;
    }

    @Override
    public String toString() {
        return getId() + " " + getTitle();
    }
}
