package com.example.demo.repository;

import com.example.demo.model.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {

    // 1. 找出特定使用者與特定成就的關聯紀錄
    Optional<UserAchievement> findByUserIdAndAchievementId(Long userId, String achievementId);

    // 2. 撈出特定使用者的所有成就進度
    List<UserAchievement> findByUserId(Long userId);

    // 3. 🎯 依解鎖時間排序（若需要展示個人「近期解鎖成就」時使用）
    List<UserAchievement> findByUserIdAndIsUnlockedTrueOrderByUnlockedAtDesc(Long userId);

    // 4. 統計該使用者目前已解鎖的成就總數（可用於成就點數排行榜或個人主頁展示）
    long countByUserIdAndIsUnlockedTrue(Long userId);
}