package com.example.demo.config;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class AppClock {

    // 預設時鐘：若要回撥 7 天，改成 Duration.ofDays(-7)；正常時間則用 Duration.ZERO
    private static Clock clock = Clock.offset(Clock.systemDefaultZone(), Duration.ofDays(-7));

    /**
     * 獲取應用程式當前時間（受時鐘偏移控制）
     */
    public static LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    /**
     * 隨時切換時鐘偏移量（方便測試或錄 Demo）
     * 例如：AppClock.setOffsetDays(-7); 或 AppClock.reset();
     */
    public static void setOffsetDays(long days) {
        clock = Clock.offset(Clock.systemDefaultZone(), Duration.ofDays(days));
    }

    public static void reset() {
        clock = Clock.systemDefaultZone();
    }

    public static Clock getClock() {
        return clock;
    }
}