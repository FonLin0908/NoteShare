package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "user_quest_progress",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "quest_id"})})
// 🛡️ 資工系防禦性聯鎖：確保同一個玩家對同一個任務，在資料庫裡只會有一筆進度紀錄
@Data
public class UserQuestProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 關聯玩家

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quest_id", nullable = false)
    private Quest quest; // 關聯任務

    @Column(nullable = false)
    private int currentCount = 0; // 當前累計次數（例如：今天已發表 1 次留言）

    @Column(nullable = false)
    private boolean isCompleted = false; // 是否達標（currentCount >= targetCount）

    @Column(nullable = false)
    private boolean isRewarded = false; // 💥 防刷關鍵：玩家是否已經手動點擊「領取獎勵」並拿到錢了

    @Column(nullable = false)
    private LocalDate lastUpdated; // 最後更新日期（用於凌晨 00:00 判定每日任務是否需要強行重置清零）

    @Column(name = "is_viewed", nullable = false)
    private boolean isViewed = false; // 🟢 預設是 false，代表新任務進來時玩家還沒看過

    public boolean getIsisViewed() { return isViewed; }
    public boolean getIsisRewarded() { return this.isRewarded; }
}