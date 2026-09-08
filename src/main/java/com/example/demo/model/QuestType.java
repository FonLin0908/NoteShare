package com.example.demo.model;

public enum QuestType {
    DAILY("每日任務", "bg-success"),
    WEEKLY("每週任務", "bg-warning text-dark"),
    EVENT("活動任務", "bg-info text-dark"),
    TUTORIAL("新手任務", "bg-primary text-white");

    private final String displayName;
    private final String cssClass;

    QuestType(String displayName, String cssClass) {
        this.displayName = displayName;
        this.cssClass = cssClass;
    }

    public String getDisplayName() { return displayName; }
    public String getCssClass() { return cssClass; }
}