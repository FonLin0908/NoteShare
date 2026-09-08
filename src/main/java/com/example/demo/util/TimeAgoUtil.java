package com.example.demo.util;

import java.time.Duration;
import java.time.LocalDateTime;

public class TimeAgoUtil {

    public static String format(LocalDateTime createdAt) {
        if (createdAt == null) return "未知時間";

        LocalDateTime now = com.example.demo.config.AppClock.now();
        Duration duration = Duration.between(createdAt, now);

        long seconds = duration.getSeconds();
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (seconds < 60) {
            return "剛剛";
        } else if (minutes < 60) {
            return minutes + " 分鐘前";
        } else if (hours < 24) {
            return hours + " 小時前";
        } else if (days < 7) {
            return days + " 天前";
        } else {
            // 超過 7 天就直接顯示標準日期格式
            return createdAt.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }
    }
}