package com.example.demo.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YouTubeParamUtil {

    // 支援一般網址、短網址、嵌入網址與 Shorts 網址。
    private static final String REGEX = "(?:youtube\\.com\\/(?:[^\\/\\n\\s]+\\/\\S+\\/|(?:v|e(?:mbed)?)\\/|\\S*?watch\\?v=)|youtu\\.be\\/)([a-zA-Z0-9_-]{11})";

    public static String extractId(String youtubeUrl) {
        if (youtubeUrl == null || youtubeUrl.trim().isEmpty()) {
            return null;
        }

        Pattern pattern = Pattern.compile(REGEX, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(youtubeUrl);

        if (matcher.find()) {
            return matcher.group(1); // 成功抓取到 11 位元的關鍵字元 ID
        }

        // 如果使用者貼的不是網址，而是本來就已經是 11 位的 ID，就原樣退回
        if (youtubeUrl.trim().length() == 11) {
            return youtubeUrl.trim();
        }

        return null; // 無法識別的格式
    }
}
