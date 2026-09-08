package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_follows", uniqueConstraints = {
        // 🔒 建立複合唯一約束，防止在資料庫中產生重複追蹤的髒資料
        @UniqueConstraint(columnNames = {"follower_id", "following_id"})
})
public class Follow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 👥 動作發出者：粉絲 (目前登入的使用者)
    @ManyToOne
    @JoinColumn(name = "follower_id", nullable = false)
    private User follower;

    // 👑 動作接收者：被追蹤的創作者 (目前主頁的主角)
    @ManyToOne
    @JoinColumn(name = "following_id", nullable = false)
    private User following;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = com.example.demo.config.AppClock.now();
    }

    // ================= Getters & Setters =================
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getFollower() { return follower; }
    public void setFollower(User follower) { this.follower = follower; }

    public User getFollowing() { return following; }
    public void setFollowing(User following) { this.following = following; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}