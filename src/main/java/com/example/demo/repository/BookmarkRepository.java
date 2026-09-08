package com.example.demo.repository;

import com.example.demo.model.Bookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    // 🔍 找尋特定使用者有沒有收藏過某篇筆記（用來切換星星空心/實心）
    Optional<Bookmark> findByUserIdAndNoteDeletedFalseAndNoteId(Long userId, Long noteId);

    // 📚 抓出特定使用者名下的所有收藏紀錄（依照時間倒序排列，拿回筆記資產）
    List<Bookmark> findByUserIdAndNoteDeletedFalseOrderByCreatedAtDesc(Long userId);
    Page<Bookmark> findByUserIdAndNoteDeletedFalseOrderByCreatedAtDesc(Long userId, Pageable pageable);


    // 📊 統計這篇筆記總共被多少人收藏
    int countByNoteId(Long noteId);

}