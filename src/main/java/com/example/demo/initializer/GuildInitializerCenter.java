package com.example.demo.initializer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GuildInitializerCenter {

    // Spring 會自動把所有實作 InitializerTask 的 Bean 全部裝進這個 List 裡！
    @Autowired
    private List<InitializerTask> initializerTasks;

    /**
     * 監聽全系統開機準備就緒事件
     */
    @Async // 將初始化工作移出啟動執行緒；工作本身仍可能在背景持續執行。
    @EventListener(ApplicationReadyEvent.class)
    public void onSystemReady() {
        System.out.println("🛰️ [公會總部] 偵測到系統啟動完畢，開始啟動全域功能加載引擎...");
        long totalStartTime = System.currentTimeMillis();

        if (initializerTasks == null || initializerTasks.isEmpty()) {
            System.out.println("ℹ️ [公會總部] 目前沒有任何功能需要初始加載。");
            return;
        }

        // 依序執行所有掛載的功能任務
        for (InitializerTask task : initializerTasks) {
            System.out.println("⏳ [核心排程] 正在啟動: " + task.getTaskName() + "...");
            long taskStartTime = System.currentTimeMillis();

            try {
                task.execute(); // 執行任務
                long taskEndTime = System.currentTimeMillis();
                System.out.println("✅ [核心排程] " + task.getTaskName() + " 執行成功，耗時: " + (taskEndTime - taskStartTime) + "ms");
            } catch (Exception e) {
                System.err.println("🚨 [核心排程] " + task.getTaskName() + " 發生致命錯誤，當前任務中斷: " + e.getMessage());
                e.printStackTrace();
            }
        }

        long totalEndTime = System.currentTimeMillis();
        System.out.println("🎉 [公會總部] 全域功能歷史數據同步校正完畢！總耗時: " + (totalEndTime - totalStartTime) + "ms");
    }
}
