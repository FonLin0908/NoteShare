package com.example.demo.repository;

import com.example.demo.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    // 查詢指定筆記中尚未軟刪除的留言，並依建立時間排序。
    List<Comment> findByNoteIdOrderByCreatedAtAsc(Long noteId);

    // 👑 聚合查詢：計算這個人蓋的所有留言的總讚數
    @Query("SELECT COALESCE(SUM(c.likesCount), 0) FROM Comment c WHERE c.user.id = :userId AND c.isDeleted = false")
    int sumLikesCountByUserId(@Param("userId") Long userId);

    // 👑 聚合查詢：計算某個用戶在特定時間後，每一天的留言發表數量
    @Query("SELECT FUNCTION('DATE', c.createdAt), COUNT(c) FROM Comment c " +
            "WHERE c.user.id = :userId AND c.createdAt >= :startDate AND c.isDeleted = false " +
            "GROUP BY FUNCTION('DATE', c.createdAt)")
    List<Object[]> countWeeklyComments(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate);

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.user.id = :userId AND c.isDeleted = false")
    int countByUserId(@Param("userId")Long userId);
    // 📄 這是先前在 CommentRepository 裡焊好的語法：
    @Modifying
    @Query("UPDATE Comment c SET c.isDeleted = true WHERE c.note.id = :noteId")
    void softDeleteAllByNoteId(@Param("noteId") Long noteId);

}
