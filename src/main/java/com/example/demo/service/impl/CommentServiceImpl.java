package com.example.demo.service.impl;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import com.example.demo.service.CommentService;
import com.example.demo.service.GamerProfileService;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final NoteRepository noteRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final UserDailyCounterRepository userDailyCounterRepository;
    @Autowired
    private GamerProfileService gamerProfileService;

    @Override
    @Transactional(readOnly = true)
    public List<Comment> getCommentsByNoteId(Long noteId) {
        // 透過 Repository 撈出該筆記旗下所有留言（已在 Repository 鎖定時間正序）
        return commentRepository.findByNoteIdOrderByCreatedAtAsc(noteId);
    }

    @Override
    @Transactional
    public Comment createComment(Long noteId, Long userId, String content) {
        // 1. 驗證字數極限（後端二次防線）
        if (content == null || content.trim().isEmpty() || content.length() > 300) {
            throw new IllegalArgumentException("評論內容必須在 1 到 300 字之間");
        }

        // 2. 撈出對應的筆記與用戶，撈不到就拋出異常
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new IllegalArgumentException("找不到該篇筆記"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("找不到該使用者"));

        // 留言只允許純文字，避免儲存型 XSS。
        // Safelist.none() 代表「全面抹除所有 HTML 標籤」，只保留純文字，最安全！
        String safeContent = Jsoup.clean(content, Safelist.none());

        // 防呆：萬一清洗完後變成空字串（代表他整篇都是惡意腳本），拋出異常
        if (safeContent.trim().isEmpty()) {
            throw new IllegalArgumentException("留言內容無效或包含不安全字串！");
        }

        // 3. 組裝留言實體
        Comment comment = new Comment();
        comment.setContent(safeContent);
        comment.setNote(note);
        comment.setUser(user);
        gamerProfileService.handleUserAction(user.getUsername(), UserAction.COMMENT);
        UserDailyCounter dailyCount = userDailyCounterRepository.findByUser(user).orElse(null);
        if (dailyCount != null) {
            dailyCount.setCommentCount(dailyCount.getCommentCount() + 1);
            userDailyCounterRepository.save(dailyCount);
        }
        return commentRepository.save(comment);
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, Long currentUserId) {
        // 1. 尋找留言
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("找不到該則留言"));

        // 2. 安全特權檢查（核心防線）
        boolean isCommentAuthor = comment.getUser().getId().equals(currentUserId);
        boolean isNoteOwner = comment.getNote().getUser().getId().equals(currentUserId);

        if (!isCommentAuthor && !isNoteOwner) {
            throw new IllegalStateException("權限不足！你既不是留言創作者，也不是筆記擁有者");
        }

        // 3. 觸發巴哈式軟刪除墓碑
        comment.setDeleted(true);

        // 💡 記憶體優化：既然被刪除了，將內文清空或維持原樣由前端遮蔽皆可，這裡我們直接儲存狀態
        commentRepository.save(comment);
    }

    @Transactional
    public Comment updateComment(Long commentId, String newContent, Long userId) {
        // 1. 撈出留言
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("找不到該則留言！"));

        // 🔒 安全防線：嚴格校正是不是作者本人在修改，防止被用 API 串改！
        if (comment.getUser() == null || !comment.getUser().getId().equals(userId)) {
            throw new IllegalStateException("你沒有權限修改這則留言！");
        }
        // 2. 檢查內文不可空白
        if (newContent == null || newContent.trim().isEmpty()) {
            throw new IllegalArgumentException("留言內容不能為空白！");
        }
        // 👑 設下第二道資安洗滌海關
        String safeNewContent = Jsoup.clean(newContent, Safelist.none());

        if (safeNewContent.trim().isEmpty()) {
            throw new IllegalArgumentException("修改內容無效或包含不安全字串！");
        }

        // 3. 更新欄位
        comment.setContent(safeNewContent);
        comment.setEdited(true); // 👑 標記為已編輯！

        return commentRepository.save(comment);
    }

    @Override
    @Transactional
    public Map<String, Object> toggleLikeComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("找不到該留言！"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("使用者不存在！"));

        Optional<CommentLike> existingLike = commentLikeRepository.findByCommentIdAndUserId(commentId, userId);

        boolean isLikedNow;

        if (existingLike.isPresent()) {
            // 💔 取消點讚
            commentLikeRepository.delete(existingLike.get());
            comment.setLikesCount(Math.max(0, comment.getLikesCount() - 1));
            isLikedNow = false;
        } else {
            // ❤️ 新增點讚
            CommentLike newLike = new CommentLike(comment, user);
            commentLikeRepository.save(newLike);
            gamerProfileService.handleUserAction(comment.getUser().getUsername(), UserAction.LIKE);
            comment.setLikesCount(comment.getLikesCount() + 1);
            isLikedNow = true;
        }

        commentRepository.save(comment);

        return Map.of(
                "likesCount", comment.getLikesCount(),
                "isLiked", isLikedNow
        );
    }
}
