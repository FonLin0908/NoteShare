package com.example.demo.repository;

import com.example.demo.model.Comment;
import com.example.demo.model.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    // 🔍 用來處理開關切換的查詢
    Optional<CommentLike> findByCommentIdAndUserId(Long commentId, Long userId);

    // 💡 核心：大樓加載時，用來判斷當前登入者有沒有點讚過這則留言
    boolean existsByCommentIdAndUserId(Long commentId, Long userId);


}