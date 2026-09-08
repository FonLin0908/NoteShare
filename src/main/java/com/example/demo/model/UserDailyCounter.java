package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "user_daily_counters")
@Data
public class UserDailyCounter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 綁定玩家

    @Column(nullable = false)
    private int noteCount = 0; // 今日已透過「發筆記」獲得獎勵的次數（上限 5）

    @Column(nullable = false)
    private int commentCount = 0; // 今日已透過「發留言」獲得獎勵的次數（上限 10）

    @Column(nullable = false)
    private LocalDate date; // 記錄日期，用來做跨日判定
}