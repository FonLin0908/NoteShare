package com.example.demo.controller;

import com.example.demo.model.Comment;
import com.example.demo.model.Note;
import com.example.demo.model.ShopItem;
import com.example.demo.model.User;
import com.example.demo.repository.CommentLikeRepository;
import com.example.demo.repository.NoteRepository;
import com.example.demo.repository.ShopItemRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.AchievementService;
import com.example.demo.service.CommentService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;
    private final CommentLikeRepository commentLikeRepository;
    private final NoteRepository noteRepository;
    private final ShopItemRepository shopItemRepository;
    private final AchievementService achievementService;

    @GetMapping("/api/comments/note/{noteId}")
    public ResponseEntity<List<Map<String, Object>>> getComments(@PathVariable Long noteId, HttpSession session) {
        List<Comment> comments = commentService.getCommentsByNoteId(noteId);
        User loginUser = (User) session.getAttribute("loginUser");

        List<Map<String, Object>> result = new ArrayList<>();
        Note note = noteRepository.findById(noteId).orElse(null);
        Long noteOwnerId = (note != null && note.getUser() != null) ? note.getUser().getId() : null;

        for (Comment comment : comments) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", comment.getId());
            map.put("content", comment.getContent());
            map.put("createdAt", comment.getCreatedAt());
            map.put("deleted", comment.isDeleted());
            map.put("edited", comment.isEdited());
            map.put("likesCount", comment.getLikesCount());
            map.put("user", comment.getUser());
            map.put("noteOwnerId", noteOwnerId);

            if (comment.getUser() != null) {
                // 1. 物理剝離外殼，還原成真正的 User 實體
                User rawUser = (User) org.hibernate.Hibernate.unproxy(comment.getUser());

                // 使用 DTO，避免序列化 JPA 實體時意外載入關聯或修改持久化狀態。
                User commenter = new User();
                commenter.setId(rawUser.getId());
                commenter.setNickname(rawUser.getNickname());
                commenter.setUsername(rawUser.getUsername());
                // ... 如果你前端還需要其他欄位（如 email），可以在這裡補上 set，確保資料不丟失 ...

                // 3. 🛡️ 智能解碼頭像框：同時相容「原廠代碼」與「被污染的 CSS 字串」
                String frameType = rawUser.getCurrentFrame();
                if (frameType != null) {
                    // 先用原廠代碼查（如 FRAME_LAVA）
                    ShopItem equippedFrame = shopItemRepository.findByAssetType(frameType);

                    if (equippedFrame != null) {
                        commenter.setCurrentFrame(equippedFrame.getStyleValue()); // 塞給前端 Class 名稱
                    } else {
                        // 💡 智能救援防線：如果查不到，代表資料庫已經被弄髒存成 CSS 字串了，那就直接把髒字串交給前端！
                        commenter.setCurrentFrame(frameType);
                    }
                }

                // 4. 🛡️ 智能解碼大頭貼：同時相容「原廠代碼」與「被污染的圖片檔名」
                String avatarType = rawUser.getCurrentAvatar();
                if (avatarType != null) {
                    // 先用原廠代碼查（如 AVATAR_CAT）
                    ShopItem equippedAvatar = shopItemRepository.findByAssetType(avatarType);

                    if (equippedAvatar != null) {
                        commenter.setCurrentAvatar(equippedAvatar.getStyleValue()); // 塞給前端圖片檔名
                    } else {
                        // 💡 智能救援防線：如果查不到，代表資料庫已經被存成圖片檔名了，直接把它交給前端！
                        commenter.setCurrentAvatar(avatarType);
                    }
                }

                // 5. 丟給前端 Map 的是我們純手工打造、跟資料庫毫無關聯的「乾淨複製品」
                map.put("user", commenter);
            } else {
                map.put("user", null);
            }

            // 👑 關鍵防線：檢查這個人有沒有點讚過這則留言
            boolean isLikedByMe = false;
            if (loginUser != null) {
                isLikedByMe = commentLikeRepository.existsByCommentIdAndUserId(comment.getId(), loginUser.getId());
            }
            map.put("isLikedByMe", isLikedByMe); // 👈 多帶這個護身符給前端！

            result.add(map);
        }

        return ResponseEntity.ok(result);
    }

    @PostMapping("/api/comments/note/{noteId}")
    public ResponseEntity<?> addComment(
            @PathVariable Long noteId,
            @RequestBody Map<String, String> payload,
            HttpSession session){
        //登入防線抓取User
        User currentUser = (User) session.getAttribute("loginUser");
        if(currentUser == null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "請先登入系統再發表評論！"));
        }
        //內容防線取出前端丟出的content文字
        String content = payload.get("content");
        try {
            Comment createdComment = commentService.createComment(noteId, currentUser.getId(), content);
            achievementService.checkAndProgressAchievement(currentUser.getId(), "COMMENT_COUNT");
            return ResponseEntity.ok(createdComment);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }

    }

    @DeleteMapping("/api/comments/{commentId}")
    public ResponseEntity<?> removeComment(@PathVariable Long commentId, HttpSession httpSession){
        //登入防線
        User user = (User) httpSession.getAttribute("loginUser");
        if(user == null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "請先登入系統！"));
        }
        try {
            commentService.deleteComment(commentId,  user.getId());
            return ResponseEntity.ok(Map.of("message", "留言已移除！"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/api/comments/{commentId}")
    public ResponseEntity<?> editComment(@PathVariable Long commentId,
                                         @RequestBody Map<String, String> payload,
                                         HttpSession session) {
        // 🔒 安全海關：確保對齊你的登入 session key "loginUser"
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "請先登入！"));
        }

        String newContent = payload.get("content");

        try {
            Comment updatedComment = commentService.updateComment(commentId, newContent, loginUser.getId());
            return ResponseEntity.ok(Map.of(
                    "message", "修改成功",
                    "content", updatedComment.getContent()
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
    @PostMapping("/api/comments/{commentId}/like")
    public ResponseEntity<?> toggleLikeComment(@PathVariable Long commentId, HttpSession session) {
        // 🔒 安全海關：對齊你的登入 session key
        User loginUser = (User) session.getAttribute("loginUser");

        if (loginUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "請先登入才能點讚！"));
        }

        try {
            Map<String, Object> result = commentService.toggleLikeComment(commentId, loginUser.getId());
            return ResponseEntity.ok(result); // 吐回最新的 { "likesCount": X, "isLiked": true/false }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

}
