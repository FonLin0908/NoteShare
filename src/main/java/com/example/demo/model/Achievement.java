package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "achievements")
public class Achievement {

    @Id
    @Column(name = "id", length = 50)
    private String id; // 例如: ACH_NOTE_01

    @Column(name = "title", nullable = false, length = 100)
    private String title; // 成就名稱

    @Column(name = "description", nullable = false)
    private String description; // 成就敘述

    @Column(name = "category", nullable = false, length = 50)
    private String category; // NOTE / COMMENT / GROWTH / HIDDEN

    @Column(name = "target_type", nullable = false, length = 50)
    private String targetType; // NOTE_COUNT / COMMENT_COUNT / USER_LEVEL 等

    @Column(name = "target_value", nullable = false)
    private Integer targetValue; // 達標門檻數字

    @Column(name = "reward_exp", nullable = false)
    private Integer rewardExp; // 獎勵經驗值

    @Column(name = "reward_coin", nullable = false)
    private Integer rewardCoin; // 獎勵金幣

    @Column(name = "reward_title", length = 50)
    private String rewardTitle; // 贈送稱號（可為空）

    @Column(name = "reward_avatar_url")
    private String rewardAvatarUrl; // 贈送限定頭像/框圖片連結（可為空）
}