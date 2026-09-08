package com.example.demo.repository;

import com.example.demo.model.UserQuestProgress;
import com.example.demo.model.User;
import com.example.demo.model.Quest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserQuestProgressRepository extends JpaRepository<UserQuestProgress, Long> {

    // 🛡️ 鋼鐵聯鎖：精準撈出「某個特定玩家」針對「某個特定任務」的當前進度紀錄
    Optional<UserQuestProgress> findByUserAndQuest(User user, Quest quest);
    List<UserQuestProgress> findAllByUserOrderByIsRewardedAscIsCompletedDescQuestTypeAsc(User user);
    // 📄 另一種防翻車選擇，貼在 UserQuestProgressRepository.java 裡面：

    @Query("SELECT uqp FROM UserQuestProgress uqp " +
            "WHERE uqp.user = :user " +
            "ORDER BY uqp.isRewarded ASC, uqp.isCompleted DESC, uqp.quest.type ASC")
    List<UserQuestProgress> findAllByUserCustomOrder(@Param("user") User user);
}