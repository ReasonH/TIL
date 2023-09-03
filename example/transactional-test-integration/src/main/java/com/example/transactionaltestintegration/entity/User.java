package com.example.transactionaltestintegration.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class User {
    @Id
    private Long id;

    private String name;

    @OneToMany(mappedBy = "user")
    private List<Post> postList = new ArrayList<>();

    public User(Long id, String name) {
        this.id = id;
        this.name = name;
    }
}
