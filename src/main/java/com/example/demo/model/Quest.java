package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "quests")
@Data // 如果不用 Lombok，請自行補上 Getter/Setter/ToString
public class Quest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title; // 任務名稱，如："活躍份子：發表 1 則留言"

    @Column(nullable = false)
    private String description; // 任務詳細敘述

    @Enumerated(EnumType.STRING) // 📢 告訴 JPA：在資料庫裡儲存為字串（"DAILY", "WEEKLY"）
    @Column(name = "type", nullable = false)
    private QuestType type = QuestType.DAILY; // 🟢 將型態從 String 改為 QuestType

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false)
    private UserAction actionType = UserAction.NOTE;

    @Column(nullable = false)
    private int targetCount; // 達標所需次數，如：1 次、3 次

    @Column(nullable = false)
    private int rewardCoins; // 獎勵金幣數量

    @Column(nullable = false)
    private int rewardExp; // 獎勵經驗值數量

    @Column(nullable = false)
    private int requiredLevel = 1; // 解鎖此任務所需的玩家最低等級限制（預設1）
}