package com.example.demo.service;

import com.example.demo.model.UserChronicle;
import com.example.demo.repository.UserChronicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserChronicleService {

    @Autowired
    private UserChronicleRepository chronicleRepository;

    /**
     * 獲取使用者的編年史列表（依時間降冪排序）
     */
    public List<UserChronicle> getUserChronicles(Long userId) {
        return chronicleRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * 🛰️ 通用非同步歷史紀錄寫入點（Async 確保不拖慢主業務速度）
     */
    @Async
    @Transactional
    public void recordEvent(Long userId, String type, String title, String content, String iconClass, String badgeColor) {
        UserChronicle chronicle = new UserChronicle();
        chronicle.setUserId(userId);
        chronicle.setType(type);
        chronicle.setTitle(title);
        chronicle.setContent(content);
        chronicle.setIconClass(iconClass != null ? iconClass : "bi-circle");
        chronicle.setBadgeColor(badgeColor != null ? badgeColor : "bg-info");
        chronicle.setCreatedAt(com.example.demo.config.AppClock.now());

        chronicleRepository.save(chronicle);
    }

    // ==================== 快捷封裝方法（各模組直接呼叫） ====================

    /**
     * 🏆 成就解鎖事件（帶 achievementId，具備防重複寫入保護）
     */
    @Async
    @Transactional
    public void recordAchievementUnlocked(Long userId, String achievementId, String achievementTitle, String description) {
        String eventType = "ACHIEVEMENT_" + achievementId;

        // 🛡️ 防重複檢查：避免重複觸發時產生多筆相同成就紀錄
        if (chronicleRepository.existsByUserIdAndType(userId, eventType)) {
            return;
        }

        recordEvent(
                userId,
                eventType,
                "達成榮譽成就【" + achievementTitle + "】",
                description,
                "bi-trophy-fill",
                "bg-warning"
        );
    }

    /**
     * 🆙 等級晉升事件（帶等級防重複寫入）
     */
    @Async
    @Transactional
    public void recordLevelUp(Long userId, int newLevel) {
        String eventType = "LEVEL_UP_" + newLevel;
        if (chronicleRepository.existsByUserIdAndType(userId, eventType)) {
            return;
        }

        recordEvent(
                userId,
                eventType,
                "冒險者等級提升至 LV." + newLevel,
                "在 NoteShare 的知識旅途中歷練成長，實力更上一層樓！",
                "bi-arrow-up-circle-fill",
                "bg-success"
        );
    }

    /**
     * 📝 筆記發表里程碑
     */
    @Async
    @Transactional
    public void recordNotePublished(Long userId, String noteTitle) {
        recordEvent(
                userId,
                "NOTE",
                "發表了全新知識筆記",
                "發布筆記《" + noteTitle + "》，為社群注入珍貴經驗。",
                "bi-journal-plus",
                "bg-info"
        );
    }

    /**
     * 🛒 黑市外觀解鎖
     */
    @Async
    @Transactional
    public void recordAssetUnlocked(Long userId, String assetDisplayName) {
        recordEvent(
                userId,
                "SHOP",
                "解鎖榮譽外觀【" + assetDisplayName + "】",
                "於榮譽商城取得全新裝扮，展現獨特冒險風采！",
                "bi-gem",
                "bg-primary"
        );
    }
}