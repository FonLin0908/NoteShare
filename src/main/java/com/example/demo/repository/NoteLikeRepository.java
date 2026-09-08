package com.example.demo.repository;

import com.example.demo.model.NoteLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NoteLikeRepository extends JpaRepository<NoteLike, Long> {
    // 檢查該使用者是否已經對該筆記點過讚
    java.util.Optional findByUserIdAndNoteId(Long userId, Long noteId);
    boolean existsByUserIdAndNoteId(Long userId, Long noteId);
    @Query("SELECT COUNT(nl) FROM NoteLike nl WHERE nl.note.user.id = :userId AND nl.note.deleted = false AND nl.note.visibility != 'PRIVATE'")
    long countValidLikesByUserId(@Param("userId") Long userId);
}