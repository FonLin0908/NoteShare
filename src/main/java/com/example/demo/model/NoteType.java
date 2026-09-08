package com.example.demo.model;

public enum NoteType {
    TECH("bg-primary", "技術筆記"),
    LIFE("bg-warning text-dark", "生活隨筆"),
    STUDY("bg-info text-dark", "課業研討"),
    MUSIC("bg-success", "音樂"),
    TEST("bg-danger", "測試用"),
    OTHER("bg-secondary", "其他記事");

    private final String cssClass;
    private final String displayName;

    NoteType(String cssClass, String displayName) {
        this.cssClass = cssClass;
        this.displayName = displayName;
    }

    public String getCssClass() { return cssClass; }
    public String getDisplayName() { return displayName; }
}