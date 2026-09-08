package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthorRankingDto {
    private String authorName;
    private String username;
    private String avatarStyle;  // 對應 user.currentAvatar
    private String frameStyle;   // 對應 user.currentFrame
    private Long totalNotes;
    private Long totalLikes;
    private Long totalBookmarks;
    private Long reputationScore;
}