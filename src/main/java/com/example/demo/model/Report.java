package com.example.demo.model; // 請根據你的專案結構調整 package

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reports")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 檢舉人
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    // 被檢舉的目標類型： "NOTE" 或 "USER"
    @Column(name = "target_type", nullable = false, length = 20)
    private String targetType;

    // 被檢舉的目標 ID（對應 Note 的 ID）
    @Column(name = "target_id", nullable = false)
    private Long targetId;

    // 檢舉原因分類
    @Enumerated(EnumType.STRING)
    @Column(name = "reason_category", nullable = false, length = 30)
    private ReportReason reasonCategory;

    // 補充說明（選填）
    @Column(columnDefinition = "TEXT")
    private String description;

    // 處理狀態：PENDING(待處理), RESOLVED(已處理)
    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = com.example.demo.config.AppClock.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getReporter() { return reporter; }
    public void setReporter(User reporter) { this.reporter = reporter; }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }

    public ReportReason getReasonCategory() { return reasonCategory; }
    public void setReasonCategory(ReportReason reasonCategory) { this.reasonCategory = reasonCategory; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
