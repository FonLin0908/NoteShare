package com.example.demo.repository;

import com.example.demo.dto.AuthorRankingDto;
import com.example.demo.model.Note;
import com.example.demo.model.NoteType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {

    // 🌟 全新新增：在公開筆記中，同時模糊搜尋「標題」或「內文」，並依最新時間排序
    List<Note> findByVisibilityAndTitleContainingOrContentContainingAndVisibilityOrderByCreatedAtDesc(
            String visibility1, String titleKeyword, String contentKeyword, String visibility2);

    // 🌟 全新新增：若使用者同時選了「分類」，就在該分類的公開筆記中進行模糊搜尋
    @Query("SELECT n FROM Note n WHERE n.visibility = :visibility " +
            "AND n.type = :type " +
            "AND n.deleted = false " +
            "AND (n.title LIKE %:keyword% OR n.content LIKE %:keyword%) " +
            "ORDER BY n.createdAt DESC")
    List<Note> findBySearchKeywordAndType(
            @Param("visibility") String visibility,
            @Param("type") NoteType type,
            @Param("keyword") String keyword
    );

    // 👑 擁有者本人視角（抓出該用戶名下所有筆記，包含公開與私密）
    List<Note> findByUserIdAndDeletedFalseOrderByCreatedAtDesc(Long userId);

    // 📜 編年史追溯專用：撈取該使用者發布的第一篇未刪除筆記（依建立時間升冪排序取第 1 筆）
    Optional<Note> findFirstByUserIdAndDeletedFalseOrderByCreatedAtAsc(Long userId);

    // 🌐 路人訪客視角（只抓出該用戶名下，且狀態為 PUBLIC 的公開筆記）
    List<Note> findByUserIdAndVisibilityAndDeletedFalseOrderByCreatedAtDesc(Long userId, String visibility);

    // 情況 A：查看大廳（只抓取公開的筆記 PUBLIC）
    List<Note> findByVisibilityAndDeletedFalseOrderByCreatedAtDesc(String visibility);
    List<Note> findByVisibilityAndDeletedFalseOrderByViewsCountDesc(String visibility);
    List<Note> findByVisibilityAndDeletedFalseOrderByLikesCountDesc(String visibility);

    // 情況 B：大廳加「分類篩選」（只抓取特定分類且公開的筆記）
    List<Note> findByVisibilityAndTypeAndDeletedFalseOrderByCreatedAtDesc(String visibility, NoteType type);
    List<Note> findByVisibilityAndTypeAndDeletedFalseOrderByViewsCountDesc(String visibility, NoteType type);
    List<Note> findByVisibilityAndTypeAndDeletedFalseOrderByLikesCountDesc(String visibility, NoteType type);

    // 👑 聚合查詢：計算這個人寫的所有筆記的總讚數（若完全沒被點讚過，Coalesce 會防呆回傳 0）
    @Query("SELECT COALESCE(SUM(n.likesCount), 0) FROM Note n WHERE n.user.id = :userId AND n.deleted = false")
    int sumLikesCountByUserId(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(n.viewsCount), 0) FROM Note n WHERE n.user.id = :userId AND n.deleted = false")
    int sumViewsCountByUserId(@Param("userId") Long userId);

    int countByUserId(Long userId);

    int countByUserIdAndDeletedFalse(Long userId);

    // 👑 聚合查詢：計算某個用戶在特定時間後，每一天的筆記發表數量
    @Query("SELECT FUNCTION('DATE', n.createdAt), COUNT(n) FROM Note n " +
            "WHERE n.user.id = :userId AND n.createdAt >= :startDate AND n.deleted = false " +
            "GROUP BY FUNCTION('DATE', n.createdAt)")
    List<Object[]> countWeeklyNotes(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate);

    // 🔍 根據神祕的加密 Token 尋找對應的筆記
    Optional<Note> findByShareToken(String shareToken);

    // 🪐 情況 A 的分頁版：大廳基礎無差別展示
    Page<Note> findByVisibilityAndDeletedFalse(String visibility, Pageable pageable);

    // 🏷️ 情況 B 的分頁版：大廳加上「分類篩選」
    Page<Note> findByVisibilityAndTypeAndDeletedFalse(String visibility, NoteType type, Pageable pageable);

    // 🔍 搜尋的分頁版：在特定分類的公開筆記中進行模糊搜尋
    @Query("SELECT n FROM Note n WHERE n.visibility = :visibility " +
            "AND n.type = :type " +
            "AND n.deleted = false " +
            "AND (n.title LIKE %:keyword% OR n.content LIKE %:keyword%)")
    Page<Note> findBySearchKeywordAndTypePaged(
            @Param("visibility") String visibility,
            @Param("type") NoteType type,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    // 🌐 全域搜尋的分頁版：不限分類，在公開筆記中同時模糊搜尋「標題」或「內文」
    @Query("SELECT n FROM Note n WHERE n.visibility = :visibility " +
            "AND n.deleted = false " +
            "AND (n.title LIKE %:keyword% OR n.content LIKE %:keyword%)")
    Page<Note> findByVisibilityAndKeywordPaged(
            @Param("visibility") String visibility,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    // 本人看自己（分頁版）
    Page<Note> findByUserIdAndDeletedFalseOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // 路人看公開（分頁版）
    Page<Note> findByUserIdAndVisibilityAndDeletedFalseOrderByCreatedAtDesc(Long userId, String visibility, Pageable pageable);

    // 1. 本週 / 本月熱門榜 (納入 SIZE(n.bookmarks) * 8 計算)
    @Query("SELECT n FROM Note n WHERE n.createdAt >= :startDate AND n.deleted = false " +
            "ORDER BY (n.viewsCount * 1 + n.likesCount * 5 + SIZE(n.bookmarks) * 8 + SIZE(n.comments) * 3) DESC")
    List<Note> findTrendingNotesAfter(@Param("startDate") LocalDateTime startDate, Pageable pageable);

    // 2. 經典殿堂榜 (全站累積熱度排序)
    @Query("SELECT n FROM Note n WHERE n.deleted = false " +
            "ORDER BY (n.viewsCount * 1 + n.likesCount * 5 + SIZE(n.bookmarks) * 8 + SIZE(n.comments) * 3) DESC")
    List<Note> findAllTimeTopNotes(Pageable pageable);

    // 3. 創作者英雄榜 (真實統計總收藏數與總聲譽分數)
    @Query("SELECT new com.example.demo.dto.AuthorRankingDto(" +
            "  COALESCE(n.user.nickname, n.user.username), " +
            "  n.user.username, " +
            "  n.user.currentAvatar, " +
            "  n.user.currentFrame, " +
            "  COUNT(n), " +
            "  COALESCE(SUM(n.likesCount), 0L), " +
            "  SUM(SIZE(n.bookmarks)), " +
            "  SUM(n.likesCount * 5 + SIZE(n.bookmarks) * 8 + SIZE(n.comments) * 3) " +
            ") " +
            "FROM Note n WHERE n.deleted = false AND n.user IS NOT NULL " +
            "GROUP BY n.user.id, n.user.nickname, n.user.username, n.user.currentAvatar, n.user.currentFrame " +
            "ORDER BY SUM(n.likesCount * 5 + SIZE(n.bookmarks) * 8 + SIZE(n.comments) * 3) DESC")
    List<AuthorRankingDto> findTopAuthors(Pageable pageable);
}