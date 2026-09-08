package com.example.demo.dto;

import lombok.Data;

@Data
public class AchievementProgressDTO {
    private String id;
    private String title;
    private String description;
    private String category;
    private Integer currentValue; // 0 / ? 的前半段
    private Integer targetValue;  // 0 / ? 的後半段
    private Boolean isUnlocked;
    private Boolean isRewardClaimed;
    private String rewardTitle;
    private String rewardAvatarUrl;
    private Integer rewardExp;
    private Integer rewardCoin;
}