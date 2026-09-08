package com.example.demo.repository;

import com.example.demo.model.Achievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AchievementRepository extends JpaRepository<Achievement, String> {

    // 即時檢查時，用來撈出某個動作（例如發筆記）對應的所有成就規則
    List<Achievement> findByTargetType(String targetType);
}