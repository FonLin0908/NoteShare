package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@ToString(exclude = "comments")
@Table(name = "users") // 對應 Supabase 中的 users 資料表
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 使用者名稱(帳號)
    @Column(unique = true, nullable = false)
    private String username;

    // 密碼
    @Column(nullable = false)
    private String password;

    // 暱稱
    private String nickname;

    // 身分組
    @Column(nullable = false)
    private String role;

    // 📅 註冊/建立時間（自動寫入，無法隨意修改）
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // RPG屬性
    @Column(nullable = false)
    private int level = 1; // 預設 1 等

    @Column(nullable = false)
    private int exp = 0;   // 預設 0 經驗值

    @Column(nullable = false)
    private int coins = 0; // 預設 0 金幣

    // 外觀
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", insertable = false, updatable = false)
    private List<UserUnlockedAsset> unlockedAssets = new ArrayList<>();

    @Column(name = "current_frame")
    private String currentFrame;

    @Column(name = "current_bg_color")
    private String currentBgColor;

    // 🟢 擴充一：當前裝備稱號 (例如: TITLE_LEGEND)
    @Column(name = "current_title")
    private String currentTitle;

    // 🟢 擴充二：當前自訂頭像 (例如: AVATAR_CYBER)
    @Column(name = "current_avatar")
    private String currentAvatar;

    // 🟢 擴充三：當前大背景圖 (例如: BG_IMAGE_DARK_SPACE)
    @Column(name = "current_bg_image")
    private String currentBgImage;

    // ==================== 💬 留言功能雙向牽線 ====================
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Comment> comments = new ArrayList<>();

    @Transient
    private String frameStyleValue;

    @Transient
    private String avatarStyleValue;

    // 🕒 實體新建時自動填充當前時間
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = com.example.demo.config.AppClock.now();
        }
    }
}