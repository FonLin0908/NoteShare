package com.example.demo.scheduler;

import com.example.demo.model.QuestType;
import com.example.demo.repository.UserDailyCounterRepository;
import com.example.demo.repository.UserQuestProgressRepository;
import com.example.demo.model.UserQuestProgress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class GameResetScheduler {

    @Autowired private UserDailyCounterRepository counterRepository;
    @Autowired private UserQuestProgressRepository progressRepository;

    /**
     * 每日午夜重置
     * 秒 分 時 日 月 週
     * "0 0 0 * * ?" 每天凌晨 00:00:00 發動
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void executeDailyReset() {
        System.out.println("偵測到0點，開始全服數據重置...");

        // 清空今日發文與留言的計數表
        counterRepository.deleteAll();
        System.out.println("💾 [重置成功] 全服玩家「每日發文/留言收益次數」已成功歸零！");

        // 遍歷所有進度，把「每日任務 (DAILY)」的計數、完成狀態、領獎狀態全部洗白
        progressRepository.findAll().forEach(progress -> {
            if (progress.getQuest() != null && progress.getQuest().getType() == QuestType.DAILY) {
                progress.setCurrentCount(0);
                progress.setCompleted(false);
                progress.setRewarded(false);
                progressRepository.save(progress);
            }
        });

        System.out.println("💾 [重置成功] 全服玩家「每日任務公告欄進度」已全面洗白翻新！");
    }
}