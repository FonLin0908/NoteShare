package com.example.demo.service;

import com.example.demo.model.*;
import com.example.demo.repository.UserDailyCounterRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.QuestRepository;
import com.example.demo.repository.UserQuestProgressRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class GamerProfileService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private QuestRepository questRepository;
    @Autowired
    private UserQuestProgressRepository progressRepository;
    @Autowired
    private UserDailyCounterRepository counterRepository;
    @Autowired
    private UserChronicleService chronicleService;

    /**
     * @param username 當前觸發事件的用戶名
     * @param actionType 動作類型："NOTE" (發筆記) 或 "COMMENT" (發表留言)
     */
    public void handleUserAction(String username, UserAction actionType) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return;

        // 🛡️ 戰線 A：處理「每日基礎行為收益與軟上限」
        processBaseRewards(user, actionType);

        // 📜 戰線 B：處理「冒險者工會任務公告欄進度疊加」
        processQuestProgress(user, actionType);
    }

    // 計算每日發文留言與上限
    private void processBaseRewards(User user, UserAction actionType) {

        // 獲取或初始化該玩家今天的計數器
        UserDailyCounter counter = counterRepository.findByUser(user)
                .orElseGet(() -> {
                    UserDailyCounter newCounter = new UserDailyCounter();
                    newCounter.setUser(user);
                    newCounter.setNoteCount(0);
                    newCounter.setCommentCount(0);
                    newCounter.setDate(LocalDate.now());
                    counterRepository.save(newCounter);
                    return newCounter;
                });

        // 如果是昨天的資料->重製
        if (!counter.getDate().equals(LocalDate.now())) {
            counter.setNoteCount(0);
            counter.setCommentCount(0);
            counter.setDate(LocalDate.now());
        }

        System.out.println(actionType.getDisplayName());
        boolean isUnderLimit = false;

        if ("發表留言".equals(actionType.getDisplayName())) {
            isUnderLimit = counter.getCommentCount() < actionType.getDailyLimit();
        } else if ("發布筆記".equals(actionType.getDisplayName())) {
            isUnderLimit = counter.getNoteCount() < actionType.getDailyLimit();
        }

        if (isUnderLimit) {
            String title = "";
            String content = "";
            String type = "";
            String iconClass = "bi-journal-check"; // 預設圖示

            if ("發表留言".equals(actionType.getDisplayName())) {
                counter.setCommentCount(counter.getCommentCount() + 1);
                title = "發表留言";
                type = "COMMENT_CREATE";
                content = "成功在社群發表留言，";
                iconClass = "bi-chat-dots-fill"; // 💬 切換為留言專屬圖示
            }
            else if ("發布筆記".equals(actionType.getDisplayName())) {
                counter.setNoteCount(counter.getNoteCount() + 1);
                title = "發表筆記";
                type = "NOTE_CREATE";
                content = "成功在社群發表筆記，";
                iconClass = "bi-journal-check";  // 📖 筆記專屬圖示
            }

            // 💰 發放獎勵
            user.setCoins(user.getCoins() + actionType.getBaseCoin());
            gainExperience(user, actionType.getBaseExp());
            userRepository.save(user);

            // 📜 組合編年史內文並寫入
            content += String.format("獲頒冒險獎勵：🪙 +%d Coin / ⚡ +%d EXP！", actionType.getBaseCoin(), actionType.getBaseExp());

            chronicleService.recordEvent(
                    user.getId(),
                    type,
                    title,
                    content,
                    iconClass, // 🎯 帶入動態切換的圖示
                    "bg-info"
            );
        }

    }

    // 刷新每日任務
    private void processQuestProgress(User user, UserAction actionType) {
        // 去字典表撈出當前進行中、目標行為符合的所有每日任務
        List<Quest> activeQuests = questRepository.findByActionType(actionType);

        for (Quest quest : activeQuests) {
            // 尋找或初始化該玩家對該任務的進度
            UserQuestProgress progress = progressRepository.findByUserAndQuest(user, quest)
                    .orElseGet(() -> {
                        UserQuestProgress newProgress = new UserQuestProgress();
                        newProgress.setUser(user);
                        newProgress.setQuest(quest);
                        newProgress.setCurrentCount(0);
                        newProgress.setCompleted(false);
                        newProgress.setRewarded(false);
                        newProgress.setViewed(false);
                        newProgress.setLastUpdated(LocalDate.now());
                        return newProgress;
                    });

            // 如果這個任務今天已經完成了，就不要重複累加次數
            if (progress.isCompleted()) {
                continue;
            }

            // 進度 +1
            progress.setCurrentCount(progress.getCurrentCount() + 1);
            progress.setLastUpdated(LocalDate.now());

            // 判定是否達標
            if (progress.getCurrentCount() >= quest.getTargetCount()) {
                progress.setCompleted(true); // 狀態機切換：觸發任務發光，等待手動領取
            }

            progressRepository.save(progress);
        }
    }

    /**
     * @param progressId 玩家在進度表 (UserQuestProgress) 中的主鍵 ID
     * @param username 當前登入的使用者帳號
     */
    //手動領取任務獎勵防護
    public void claimQuestReward(Long progressId, String username) {
        // 校驗這筆進度紀錄必須存在
        UserQuestProgress progress = progressRepository.findById(progressId)
                .orElseThrow(() -> new RuntimeException("系統找不到此項紀錄！"));

        // 點擊領獎的人，必須跟這筆紀錄的主人一模一樣（防範竄改網頁 ID 幫別人領獎）
        if (!progress.getUser().getUsername().equals(username)) {
            throw new RuntimeException("你無權領取屬於其他人的獎勵！");
        }

        // 必須「已完成 (isCompleted=true)」且「尚未領過 (isRewarded=false)」
        if (!progress.isCompleted()) {
            throw new RuntimeException("任務目標尚未達成，無法領取獎勵");
        }
        if (progress.isRewarded()) {
            throw new RuntimeException("這筆獎勵你今天已經領取過了！");
        }

        // 檢查完成
        User user = progress.getUser();
        Quest quest = progress.getQuest();

        user.setCoins(user.getCoins() + quest.getRewardCoins());
        gainExperience(user, quest.getRewardExp());

        // 將領取狀態永久切換為 true，封死重複洗錢的漏洞
        progress.setRewarded(true);

        String title = "完成任務【" + quest.getTitle() + "】";

        // 組合內容：包含任務名稱與獲取的獎勵
        String content = String.format("成功完成%s，獲頒冒險獎勵：🪙 +%d Coin / ⚡ +%d EXP！",
                progress.getQuest().getType().getDisplayName(),
                quest.getRewardCoins(),
                quest.getRewardExp()
        );

        // 如果任務有額外獎勵（例如稱號或道具），可以繼續追加
//        if (quest.getRewardTitle() != null && !quest.getRewardTitle().isBlank()) {
//            content += " 獲得限定稱號：【" + quest.getRewardTitle() + "】";
//        }

        // 寫入編年史
        chronicleService.recordEvent(
                user.getId(),
                "QUEST_COMPLETE",           // type: 任務完成事件
                title,                      // title: 完成任務【每日簽到】
                content,                    // content: 獲頒冒險獎勵：...
                "bi-award-fill",            // iconClass: 勳章/獎牌圖示
                "bg-warning"                // badgeColor: 亮黃色（代表榮譽獎勵）
        );

        // 升級檢查
        //checkLevelUp(user);

        userRepository.save(user);
        progressRepository.saveAndFlush(progress); // 強行 Flush 確保狀態立刻鎖死
    }

    /**
     * 🟢 數學模型二門檻：輸入目標等級，計算生涯所需的「累積總經驗值」
     * 公式：每級級距 = 50 * L^2 + 100 * L
     */
    public long getRequiredTotalExpForLevel(int targetLevel) {
        if (targetLevel <= 1) return 0;
        long totalRequired = 0;
        for (int l = 1; l < targetLevel; l++) {
            totalRequired += (50L * l * l) + (100L * l);
        }
        return totalRequired;
    }

    /**
     * 🟢 核心逆算：輸入生涯總經驗值，現場秒算應該是多少等級
     */
    public int calculateLevelFromTotalExp(long totalExp) {
        if (totalExp <= 0) return 1;
        int level = 1;
        while (totalExp >= getRequiredTotalExpForLevel(level + 1)) {
            level++;
            if (level >= 100) break; // 安全上限
        }
        return level;
    }

    /**
     * 增加玩家總經驗值，重新計算等級並發放跨級獎勵。
     */
    @Transactional
    public void gainExperience(User user, int amount) {
        if (amount <= 0) return;

        // exp 儲存累積總經驗值，而不是目前等級內的剩餘經驗。
        long newTotalExp = user.getExp() + amount;
        user.setExp((int) newTotalExp);

        int newLevel = calculateLevelFromTotalExp(newTotalExp);

        if (newLevel > user.getLevel()) {
            int oldLevel = user.getLevel();
            user.setLevel(newLevel); // 更新快取欄位 level

            handleLevelUp(user, oldLevel, newLevel);
        }

        userRepository.save(user);
    }

    /** 發放一次跨級事件涵蓋的全部金幣獎勵。 */
    private void handleLevelUp(User user, int oldLevel, int newLevel) {
        System.out.println("🎉 [RANK UP] " + user.getNickname() + " 晉升到了 Lv." + newLevel);

        // 每提升一級獎勵 50 金幣；一次跨越多級時按級數累計。
        int bonus = (newLevel - oldLevel) * 50;
        user.setCoins(user.getCoins() + bonus);
    }
}
