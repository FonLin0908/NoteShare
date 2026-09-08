package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "note_likes",
        uniqueConstraints = {
                // 🔒 鐵律：同一個使用者對同一篇筆記，在資料庫裡只能有一筆點讚紀錄，物理上杜絕刷讚
                @UniqueConstraint(columnNames = {"user_id", "note_id"})
        })
@Data
public class NoteLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "note_id", nullable = false)
    private Note note;
}