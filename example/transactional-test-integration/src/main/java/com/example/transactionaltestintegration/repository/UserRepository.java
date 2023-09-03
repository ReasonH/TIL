package com.example.transactionaltestintegration.repository;

import com.example.transactionaltestintegration.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("select distinct u from User u join fetch u.postList")
    List<User> findAllByJoinFetch();
}
