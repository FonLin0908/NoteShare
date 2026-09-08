package com.example.demo.service;

import com.example.demo.dto.AuthorRankingDto;
import com.example.demo.model.Note;
import com.example.demo.repository.NoteRepository;
import com.example.demo.repository.ShopItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RankingService {

    @Autowired
    private NoteRepository noteRepository;
    @Autowired
    private ShopItemRepository shopItemRepository;

    // 取得週熱門榜 (Top 20)
    public List<Note> getWeeklyTopNotes() {
        LocalDateTime oneWeekAgo = com.example.demo.config.AppClock.now().minusDays(7);
        return noteRepository.findTrendingNotesAfter(oneWeekAgo, PageRequest.of(0, 20));
    }

    // 取得月熱門榜 (Top 20)
    public List<Note> getMonthlyTopNotes() {
        LocalDateTime oneMonthAgo = com.example.demo.config.AppClock.now().minusDays(30);
        return noteRepository.findTrendingNotesAfter(oneMonthAgo, PageRequest.of(0, 20));
    }

    // 取得總經典殿堂榜 (Top 20)
    public List<Note> getAllTimeTopNotes() {
        return noteRepository.findAllTimeTopNotes(PageRequest.of(0, 20));
    }

    // 取得創作者英雄榜 (Top 20)
    public List<AuthorRankingDto> getTopAuthors() {
        List<AuthorRankingDto> authors = noteRepository.findTopAuthors(PageRequest.of(0, 20));

        for (AuthorRankingDto author : authors) {
            // 如果有裝備頭像代碼，向 ShopItem 查詢真實檔名
            if (author.getAvatarStyle() != null) {
                var avatarItem = shopItemRepository.findByAssetType(author.getAvatarStyle());
                if (avatarItem != null) {
                    // 將 avatarStyle 替換成真實檔名 (例如 cyber.png)
                    author.setAvatarStyle(avatarItem.getStyleValue());
                }
            }
            // 如果有裝備頭像框代碼，向 ShopItem 查詢 CSS 類別
            if (author.getFrameStyle() != null) {
                var frameItem = shopItemRepository.findByAssetType(author.getFrameStyle());
                if (frameItem != null) {
                    author.setFrameStyle(frameItem.getStyleValue());
                }
            }
        }

        return authors;
    }
}