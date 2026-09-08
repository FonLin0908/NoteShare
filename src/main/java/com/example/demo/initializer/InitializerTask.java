package com.example.demo.initializer;

public interface InitializerTask {
    void execute();      // 任務執行的核心邏輯
    String getTaskName(); // 任務名稱（方便看 Log 監控）
}