package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_unlocked_assets", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "asset_type"})
})
public class UserUnlockedAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "asset_type", nullable = false)
    private String assetType;

    @Column(name = "unlocked_at", updatable = false)
    private LocalDateTime unlockedAt = com.example.demo.config.AppClock.now();

    @PrePersist
    protected void onCreate() {
        if (this.unlockedAt == null) {
            this.unlockedAt = com.example.demo.config.AppClock.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getAssetType() { return assetType; }
    public void setAssetType(String assetType) { this.assetType = assetType; }
    public LocalDateTime getUnlockedAt() { return unlockedAt; }
    public void setUnlockedAt(LocalDateTime unlockedAt) { this.unlockedAt = unlockedAt; }
}
