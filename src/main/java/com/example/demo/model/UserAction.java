package com.example.demo.model;

public enum UserAction {
    // 定義：(行為中文名稱, 基礎經驗值獎勵, 每日限制次數)
    NOTE("發布筆記", 200,50 ,5),
    COMMENT("發表留言", 50, 10, 10),
    LIKE("按讚", 0, 0, 0),
    CLICK_AD("觀看贊助", 10, 20, 3);

    private final String displayName;
    private final int baseExp;
    private final int baseCoin;
    private final int dailyLimit;

    UserAction(String displayName, int baseExp,int baseCoin, int dailyLimit) {
        this.displayName = displayName;
        this.baseExp = baseExp;
        this.baseCoin = baseCoin;
        this.dailyLimit = dailyLimit;
    }

    public String getDisplayName() { return displayName; }
    public int getBaseExp() { return baseExp; }
    public int getBaseCoin() { return baseCoin; }
    public int getDailyLimit() { return dailyLimit; }
}