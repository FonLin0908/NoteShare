package com.example.demo.initializer;

import com.example.demo.model.Achievement;
import com.example.demo.model.Note;
import com.example.demo.model.User;
import com.example.demo.model.UserAchievement;
import com.example.demo.model.UserChronicle;
import com.example.demo.repository.NoteRepository;
import com.example.demo.repository.UserAchievementRepository;
import com.example.demo.repository.UserChronicleRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class ChronicleInitializerTask implements InitializerTask {

    @Autowired
    private UserChronicleRepository chronicleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private UserAchievementRepository userAchievementRepository;

    @Override
    public String getTaskName() {
        return "編年史系統 - 歷史里程碑追溯任務";
    }

    @Override
    @Transactional
    public void execute() {
        System.out.println("📜 [編年史系統] 開始掃描並追溯玩家過去的歷史事件與成就...");
        long startTime = System.currentTimeMillis();

        // 1. 撈出系統中所有冒險者
        List<User> allUsers = userRepository.findAll();
        if (allUsers.isEmpty()) {
            System.out.println("⚠️ [編年史系統] 查無使用者，略過追溯。");
            return;
        }

        for (User user : allUsers) {
            Long userId = user.getId();

            // -------------------------------------------------------------
            // 🎯 追溯事件 A：創角／註冊里程碑
            // -------------------------------------------------------------
            boolean hasRegisterEvent = chronicleRepository.existsByUserIdAndType(userId, "SYSTEM_REGISTER");
            if (!hasRegisterEvent) {
                LocalDateTime registerTime = (user.getCreatedAt() != null)
                        ? user.getCreatedAt()
                        : com.example.demo.config.AppClock.now().minusDays(30);

                UserChronicle registerChronicle = new UserChronicle();
                registerChronicle.setUserId(userId);
                registerChronicle.setType("SYSTEM_REGISTER");
                registerChronicle.setTitle("踏入 NoteShare 冒險者公會");
                registerChronicle.setContent("正式註冊成為 NoteShare 社群會員，開啟知識冒險者之旅！");
                registerChronicle.setIconClass("bi-door-open-fill");
                registerChronicle.setBadgeColor("bg-primary");
                registerChronicle.setCreatedAt(registerTime);

                chronicleRepository.save(registerChronicle);
            }

            // -------------------------------------------------------------
            // 🎯 追溯事件 B：第一篇筆記發表里程碑
            // -------------------------------------------------------------
            boolean hasFirstNoteEvent = chronicleRepository.existsByUserIdAndType(userId, "FIRST_NOTE");
            if (!hasFirstNoteEvent) {
                Optional<Note> firstNoteOpt = noteRepository.findFirstByUserIdAndDeletedFalseOrderByCreatedAtAsc(userId);

                if (firstNoteOpt.isPresent()) {
                    Note firstNote = firstNoteOpt.get();
                    LocalDateTime noteTime = (firstNote.getCreatedAt() != null)
                            ? firstNote.getCreatedAt()
                            : com.example.demo.config.AppClock.now();

                    UserChronicle noteChronicle = new UserChronicle();
                    noteChronicle.setUserId(userId);
                    noteChronicle.setType("FIRST_NOTE");
                    noteChronicle.setTitle("發表首篇筆記《" + firstNote.getTitle() + "》");
                    noteChronicle.setContent("首次在公會發表個人筆記，邁出成為高級冒險者的第一步。");
                    noteChronicle.setIconClass("bi-journal-plus");
                    noteChronicle.setBadgeColor("bg-success");
                    noteChronicle.setCreatedAt(noteTime);

                    chronicleRepository.save(noteChronicle);
                }
            }

            // -------------------------------------------------------------
            // 🎯 追溯事件 C：歷史已達成成就追溯里程碑 (全新新增)
            // -------------------------------------------------------------
            List<UserAchievement> userAchievements = userAchievementRepository.findByUserId(userId);

            for (UserAchievement ua : userAchievements) {
                // 只有已解鎖的成就才寫入編年史
                if (Boolean.TRUE.equals(ua.getIsUnlocked()) && ua.getAchievement() != null) {
                    Achievement ach = ua.getAchievement();
                    String chronicleType = "ACHIEVEMENT_" + ach.getId();

                    // 防重複寫入檢查
                    boolean hasAchEvent = chronicleRepository.existsByUserIdAndType(userId, chronicleType);
                    if (!hasAchEvent) {
                        LocalDateTime unlockTime = (ua.getUnlockedAt() != null)
                                ? ua.getUnlockedAt()
                                : com.example.demo.config.AppClock.now();

                        UserChronicle achChronicle = new UserChronicle();
                        achChronicle.setUserId(userId);
                        achChronicle.setType(chronicleType);
                        achChronicle.setTitle("達成榮譽成就【" + ach.getTitle() + "】");
                        achChronicle.setContent(ach.getDescription());
                        achChronicle.setIconClass("bi-trophy-fill");
                        achChronicle.setBadgeColor("bg-warning");
                        achChronicle.setCreatedAt(unlockTime);

                        chronicleRepository.save(achChronicle);
                    }
                }
            }
        }

        long endTime = System.currentTimeMillis();
        System.out.println("🎉 [編年史系統] 歷史里程碑與成就追溯完畢！總耗時: " + (endTime - startTime) + "ms");
    }
}