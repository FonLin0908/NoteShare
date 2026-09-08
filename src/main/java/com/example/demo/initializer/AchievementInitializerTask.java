package com.example.demo.initializer;

import com.example.demo.model.Achievement;
import com.example.demo.model.User;
import com.example.demo.model.UserAchievement;
import com.example.demo.model.UserUnlockedAsset;
import com.example.demo.repository.AchievementRepository;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.NoteRepository;
import com.example.demo.repository.UserAchievementRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.UserUnlockedAssetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class AchievementInitializerTask implements InitializerTask {

    @Autowired
    private AchievementRepository achievementRepository;

    @Autowired
    private UserAchievementRepository userAchievementRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private UserUnlockedAssetRepository userUnlockedAssetRepository;

    @Override
    public String getTaskName() {
        return "成就系統 - 歷史數據追溯與預熱任務";
    }

    @Override
    @Transactional
    public void execute() {
        System.out.println("🛰️ [成就系統] 開始執行啟動加載與歷史進度同步...");
        long startTime = System.currentTimeMillis();

        List<Achievement> allAchievements = achievementRepository.findAll();
        if (allAchievements.isEmpty()) {
            System.out.println("⚠️ [成就系統] 尚未建立任何成就規則，跳過同步。");
            return;
        }

        List<User> allUsers = userRepository.findAll();

        for (User user : allUsers) {
            Long userId = user.getId();

            // 統計該使用者真實歷史數據
            int actualNoteCount = noteRepository.countByUserIdAndDeletedFalse(userId);
            int actualCommentCount = (int) commentRepository.countByUserId(userId);
            int actualUserLevel = user.getLevel();

            for (Achievement ach : allAchievements) {
                int actualValue = 0;
                switch (ach.getTargetType()) {
                    case "NOTE_COUNT" -> actualValue = actualNoteCount;
                    case "COMMENT_COUNT" -> actualValue = actualCommentCount;
                    case "USER_LEVEL" -> actualValue = actualUserLevel;
                    default -> { continue; } // 彩蛋或即時時間型成就跳過歷史推算
                }

                UserAchievement userProgress = userAchievementRepository
                        .findByUserIdAndAchievementId(userId, ach.getId())
                        .orElseGet(() -> {
                            UserAchievement newProgress = new UserAchievement();
                            newProgress.setUserId(userId);
                            newProgress.setAchievement(ach);
                            newProgress.setCurrentValue(0);
                            newProgress.setIsUnlocked(false);
                            return newProgress;
                        });

                // 歷史數據大於目前進度且未解鎖時更新
                if (Boolean.FALSE.equals(userProgress.getIsUnlocked()) && actualValue > userProgress.getCurrentValue()) {
                    userProgress.setCurrentValue(Math.min(actualValue, ach.getTargetValue()));

                    if (actualValue >= ach.getTargetValue()) {
                        userProgress.setIsUnlocked(true);
                        userProgress.setUnlockedAt(com.example.demo.config.AppClock.now());

                        // 補發稱號與外觀資產
                        grantHistoricalAssets(userId, ach);
                    }
                    userAchievementRepository.save(userProgress);
                }
            }
        }

        long endTime = System.currentTimeMillis();
        System.out.println("🎉 [成就系統] 啟動進度同步完畢！總耗時: " + (endTime - startTime) + "ms");
    }

    private void grantHistoricalAssets(Long userId, Achievement ach) {
        if (ach.getRewardTitle() != null && !ach.getRewardTitle().trim().isEmpty()) {
            unlockAssetIfNotExists(userId, ach.getRewardTitle().trim());
        }
        if (ach.getRewardAvatarUrl() != null && !ach.getRewardAvatarUrl().trim().isEmpty()) {
            unlockAssetIfNotExists(userId, ach.getRewardAvatarUrl().trim());
        }
    }

    private void unlockAssetIfNotExists(Long userId, String assetType) {
        boolean exists = userUnlockedAssetRepository.findByUserIdAndAssetType(userId, assetType).isPresent();
        if (!exists) {
            UserUnlockedAsset asset = new UserUnlockedAsset();
            asset.setUserId(userId);
            asset.setAssetType(assetType);
            userUnlockedAssetRepository.save(asset);
        }
    }
}