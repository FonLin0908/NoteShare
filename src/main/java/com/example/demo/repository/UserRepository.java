package com.example.demo.repository;

import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // 透過帳號查詢使用者，使用 Optional 可以優雅地處理「找不到帳號」的情況
    Optional<User> findByUsername(String username);
}