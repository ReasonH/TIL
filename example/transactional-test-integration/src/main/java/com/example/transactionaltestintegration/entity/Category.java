package com.example.transactionaltestintegration.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Category {
    @Id
    private Long id;

    private String title;

    public Category(Long id, String title) {
        this.id = id;
        this.title = title;
    }

    @Override
    public String toString() {
        return getTitle();
    }
}
