package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "user_achievements",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "achievement_id"})})
public class UserAchievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId; // 關聯使用者 ID (對應你專案的使用者識別碼類型)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "achievement_id", nullable = false)
    private Achievement achievement; // 關聯成就定義

    @Column(name = "current_value", nullable = false)
    private Integer currentValue = 0; // 目前達到的數值（例如發了幾篇筆記），就是 0/? 的前半段

    @Column(name = "is_unlocked", nullable = false)
    private Boolean isUnlocked = false; // 是否已解鎖

    @Column(name = "is_reward_claimed", nullable = false)
    private Boolean isRewardClaimed = false; // 是否已領取獎勵

    @Column(name = "unlocked_at")
    private LocalDateTime unlockedAt; // 解鎖時間（即時檢查寫入）
}