package com.example.demo.service;

import com.example.demo.model.Comment;
import java.util.List;
import java.util.Map;

public interface CommentService {

    // 💡 根據筆記 ID 撈出該筆記旗下的所有留言（提供前端大樓渲染）
    List<Comment> getCommentsByNoteId(Long noteId);

    // 💡 發表新留言：需要知道是誰留的、留在哪篇、內容是什麼
    Comment createComment(Long noteId, Long userId, String content);

    // 💡 巴哈式軟刪除：需要知道是誰想刪（執行權限檢查），以及要刪哪一條
    void deleteComment(Long commentId, Long currentUserId);

    Map<String, Object> toggleLikeComment(Long commentId, Long userId);
    Comment updateComment(Long commentId, String newContent, Long userId);
}