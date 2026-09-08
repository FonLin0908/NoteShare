package com.example.demo.model; // 請根據你的專案結構調整 package

public enum ReportReason {
    PLAGIARISM("抄襲、侵權"),
    SPAM("垃圾內容與廣告"),
    INAPPROPRIATE("不當或成人內容"),
    DUPLICATE("重複上傳");

    private final String displayName;

    ReportReason(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}