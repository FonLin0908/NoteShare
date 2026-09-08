package com.example.demo.service;

import com.example.demo.config.AppClock;
import com.example.demo.dto.AchievementProgressDTO;
import com.example.demo.model.Achievement;
import com.example.demo.model.Note;
import com.example.demo.model.User;
import com.example.demo.model.UserAchievement;
import com.example.demo.model.UserUnlockedAsset;
import com.example.demo.repository.AchievementRepository;
import com.example.demo.repository.NoteRepository;
import com.example.demo.repository.UserAchievementRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.UserUnlockedAssetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AchievementService {

    @Autowired
    private AchievementRepository achievementRepository;

    @Autowired
    private UserAchievementRepository userAchievementRepository;

    @Autowired
    private UserUnlockedAssetRepository userUnlockedAssetRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private GamerProfileService gamerProfileService;

    @Autowired
    private NoteRepository noteRepository;

    // 📜 注入冒險者編年史服務
    @Autowired
    private UserChronicleService userChronicleService;

    /**
     * 即時檢查與更新成就進度 (按次數累計型，例如: 發文數、留言數)
     * @param userId 當前操作的使用者 ID
     * @param targetType 觸發類型 (例如: "NOTE_COUNT", "COMMENT_COUNT")
     */
    @Transactional
    public void checkAndProgressAchievement(Long userId, String targetType) {
        // 1. 撈出該觸發類型所有的成就規則
        List<Achievement> relatedAchievements = achievementRepository.findByTargetType(targetType);

        for (Achievement achievement : relatedAchievements) {
            // 2. 檢查使用者是否有這項成就的紀錄，沒有就初始化一筆
            UserAchievement userProgress = userAchievementRepository
                    .findByUserIdAndAchievementId(userId, achievement.getId())
                    .orElseGet(() -> {
                        UserAchievement newProgress = new UserAchievement();
                        newProgress.setUserId(userId);
                        newProgress.setAchievement(achievement);
                        newProgress.setCurrentValue(0);
                        newProgress.setIsUnlocked(false);
                        return newProgress;
                    });

            // 3. 如果已經解鎖過，累積型的就不用再重複處理
            if (Boolean.TRUE.equals(userProgress.getIsUnlocked())) {
                continue;
            }

            // 4. 即時更新進度值 (+1)
            int newValue = userProgress.getCurrentValue() + 1;
            userProgress.setCurrentValue(newValue);

            // 5. 判定是否達到解鎖門檻
            if (newValue >= achievement.getTargetValue()) {
                userProgress.setIsUnlocked(true);
                userProgress.setUnlockedAt(AppClock.now());

                // 6. 觸發發放獎勵機制（內部會自動通知編年史）
                grantReward(userId, achievement);
            }

            // 7. 存回資料庫
            userAchievementRepository.save(userProgress);
        }
    }

    /**
     * 即時檢查與更新筆記關聯成就進度 (數值覆蓋型，例如: 筆記獲得的讚數)
     * @param noteID 筆記 ID
     * @param targetType 觸發類型 (例如: "NOTE_LIKES")
     */
    @Transactional
    public void checkAndProgressAchievementNote(Long noteID, String targetType) {
        List<Achievement> relatedAchievements = achievementRepository.findByTargetType(targetType);

        Note note = noteRepository.findById(noteID).orElse(null);
        if (note != null && note.getUser() != null) {
            long userId = note.getUser().getId();
            for (Achievement achievement : relatedAchievements) {
                UserAchievement userProgress = userAchievementRepository
                        .findByUserIdAndAchievementId(userId, achievement.getId())
                        .orElseGet(() -> {
                            UserAchievement newProgress = new UserAchievement();
                            newProgress.setUserId(userId);
                            newProgress.setAchievement(achievement);
                            newProgress.setCurrentValue(0);
                            newProgress.setIsUnlocked(false);
                            return newProgress;
                        });

                if (Boolean.TRUE.equals(userProgress.getIsUnlocked())) {
                    continue;
                }

                // 依據筆記當前的最新讚數更新
                int newValue = note.getLikesCount();
                userProgress.setCurrentValue(newValue);

                if (newValue >= achievement.getTargetValue()) {
                    userProgress.setIsUnlocked(true);
                    userProgress.setUnlockedAt(AppClock.now());

                    grantReward(userId, achievement);
                }

                userAchievementRepository.save(userProgress);
            }
        }
    }

    /**
     * 🎁 發放成就獎勵（EXP、金幣、稱號與限定外觀），並同步寫入編年史
     */
    private void grantReward(Long userId, Achievement achievement) {
        User user = userService.findById(userId);
        if (user == null) {
            return;
        }

        // 1. 經驗值與金幣增益
        if (achievement.getRewardExp() != null && achievement.getRewardExp() > 0) {
            gamerProfileService.gainExperience(user, achievement.getRewardExp());
        }
        if (achievement.getRewardCoin() != null && achievement.getRewardCoin() > 0) {
            user.setCoins(user.getCoins() + achievement.getRewardCoin());
            userRepository.save(user);
        }

        // 2. 稱號發放落庫 (寫入使用者解鎖外觀資產表)
        if (achievement.getRewardTitle() != null && !achievement.getRewardTitle().trim().isEmpty()) {
            unlockAssetIfNotExists(userId, achievement.getRewardTitle().trim());
            System.out.println("【成就解鎖】獲得新稱號：" + achievement.getRewardTitle());
        }

        // 3. 限定頭像/外觀發放落庫 (例如: "AVATAR_MIDNIGHT_RAMEN")
        if (achievement.getRewardAvatarUrl() != null && !achievement.getRewardAvatarUrl().trim().isEmpty()) {
            unlockAssetIfNotExists(userId, achievement.getRewardAvatarUrl().trim());
            System.out.println("【成就解鎖】獲得限定外觀：" + achievement.getRewardAvatarUrl());
        }

        // 📜 4. 寫入冒險者編年史時間軸
        userChronicleService.recordAchievementUnlocked(
                userId,
                achievement.getId(),
                achievement.getTitle(),
                achievement.getDescription()
        );

        System.out.println("【成就解鎖通知】使用者 " + userId + " 成功解鎖成就：" + achievement.getTitle());
    }

    /**
     * 🛡️ 輔助方法：外觀/稱號安全入庫防重複
     */
    private void unlockAssetIfNotExists(Long userId, String assetType) {
        boolean alreadyOwned = userUnlockedAssetRepository
                .findByUserIdAndAssetType(userId, assetType)
                .isPresent();

        if (!alreadyOwned) {
            UserUnlockedAsset newAsset = new UserUnlockedAsset();
            newAsset.setUserId(userId);
            newAsset.setAssetType(assetType);
            newAsset.setUnlockedAt(AppClock.now()); // 👈 記錄當下獲得時間
            userUnlockedAssetRepository.save(newAsset);
        }
    }

    /**
     * 獲取使用者在所有成就中的當前進度列表，供前端成就殿堂牆渲染
     */
    public List<AchievementProgressDTO> getUserAchievementList(Long userId) {
        List<Achievement> allAchievements = achievementRepository.findAll();

        // 🎯 核心修復：改撈取該玩家的「全部成就進度」（包含未解鎖的），進行中的進度（如 1/10）才不會被濾除
        List<UserAchievement> userRecords = userAchievementRepository.findByUserId(userId);

        return allAchievements.stream().map(achievement -> {
            AchievementProgressDTO dto = new AchievementProgressDTO();
            dto.setId(achievement.getId());
            dto.setTitle(achievement.getTitle());
            dto.setDescription(achievement.getDescription());
            dto.setCategory(achievement.getCategory());
            dto.setTargetValue(achievement.getTargetValue());

            // 成就基礎獎勵資訊：統一套用在最外層，確保未解鎖時也能正常看到數值
            dto.setRewardCoin(achievement.getRewardCoin());
            dto.setRewardExp(achievement.getRewardExp());
            dto.setRewardTitle(achievement.getRewardTitle());
            dto.setRewardAvatarUrl(achievement.getRewardAvatarUrl());

            // 尋找使用者是否有這項成就的進度紀錄（加上 null 安全檢查）
            Optional<UserAchievement> recordOpt = userRecords.stream()
                    .filter(r -> r.getAchievement() != null && r.getAchievement().getId().equals(achievement.getId()))
                    .findFirst();

            if (recordOpt.isPresent()) {
                UserAchievement record = recordOpt.get();
                dto.setCurrentValue(record.getCurrentValue()); // 👈 讀取真實進度 (如 1)
                dto.setIsUnlocked(record.getIsUnlocked());
                dto.setIsRewardClaimed(record.getIsRewardClaimed());
            } else {
                dto.setCurrentValue(0);
                dto.setIsUnlocked(false);
                dto.setIsRewardClaimed(false);
            }

            return dto;
        }).collect(Collectors.toList());
    }
}