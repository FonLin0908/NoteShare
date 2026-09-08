package com.example.demo.repository;

import com.example.demo.model.UserDailyCounter;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserDailyCounterRepository extends JpaRepository<UserDailyCounter, Long> {
    Optional<UserDailyCounter> findByUser(User user);
}