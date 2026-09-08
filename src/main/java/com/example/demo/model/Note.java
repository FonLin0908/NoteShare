package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@ToString(exclude = "comments")
@Table(name = "notes") // 對應 Supabase 中的 notes 資料表
public class Note {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 1. 基礎欄位
    @Column(nullable = false)
    private String title;       // 筆記名稱

    @Column(columnDefinition = "TEXT")
    private String content;     // 筆記具體內容（大文字區域）

    @Enumerated(EnumType.STRING)
    @Column(name = "note_type")
    private NoteType type = NoteType.OTHER;


    private String tags;        // 標籤，暫時用逗號隔開 (例如："RSA,資安,期中考")

    // 2. 視覺與時間欄位
    private String coverImageUrl; // 預覽圖片的網址 (如果不填，前端可以給預設圖)
    // === 以下是全新追加的富文本多媒體欄位 ===

    // 3. 一篇筆記可以擁有「多張圖片」 (OneToMany)
    // cascade = CascadeType.ALL 代表如果這篇筆記被刪除，底下的圖片也會連帶一起刪除
    @OneToMany(mappedBy = "note", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<NoteImage> images = new java.util.ArrayList<>();

    // 4. 外部連結：YouTube 影片嵌入代碼 (可空)
    private String youtubeVideoId; // 我們只存影片 ID，例如 "dQw4w9WgXcQ"

    // 5. 附件檔案：其他檔案內容的下載連結 (可空)
    private String attachmentUrl;
    private String attachmentFileName; // 檔案名稱 (例如 "期末考古題.pdf")

    @Column(nullable = false)
    private LocalDateTime createdAt; // 發布時間（用來計算「幾分鐘前」）

    // 6. 隱私狀態：使用字串來區分 (PUBLIC: 公開, PRIVATE: 不公開, UNLISTED: 半公開/僅限連結查看)
    @Column(nullable = false)
    private String visibility = "PUBLIC"; // 預設為公開

    @Column(name = "share_token", unique = true, length = 64)
    private String shareToken;

    // 7. 記錄這篇筆記是哪個「使用者」建立的
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // 8. 全新欄位：為了實現「半公開（有連結才可以看見）」，我們幫每篇筆記生成一個不重複的隨機代碼（UUID）
    // 這樣別人就無法透過猜測 id (例如 id=1, id=2) 來偷看半公開的筆記
    @Column(unique = true)
    private String shareCode;

    // 9. 排序與熱門度欄位（預設值皆為 0）
    private int viewsCount = 0;   // 點閱次數 (點擊率)
    private int likesCount = 0;   // 推薦數 (熱門度)

    @PrePersist
    protected void onCreate() {
        // 在資料寫入 Supabase 前，自動抓取當前時間作為發布時間
        this.createdAt = com.example.demo.config.AppClock.now();

        // 在資料初次寫入 Supabase 前，自動生成隨機的 shareCode
        if (this.shareCode == null) {
            this.shareCode = java.util.UUID.randomUUID().toString().replace("-", "");
        }
    }
    // ==================== 💬 留言功能雙向牽線 ====================

    // 💡 透過 mappedBy 告訴 JPA：這條線的發言權（外鍵維護）在 Comment 類別的 note 欄位上
    // FetchType.LAZY 保持延遲載入，CascadeType.ALL 確保筆記如果被刪除，地盤下的留言也一併清空
    @OneToMany(mappedBy = "note", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JsonIgnore
    @OrderBy("createdAt ASC")
    private List<Comment>  comments = new ArrayList<>();

    private boolean deleted = false;

    // 取得留言數量 (給 Thymeleaf 排行榜使用)
    public int getCommentCount() {
        return this.comments != null ? this.comments.size() : 0;
    }

    // 取得作者名字 (給 Thymeleaf 排行榜使用)
    // 取得作者名字 (優先回傳暱稱，給 Thymeleaf 排行榜使用)
    public String getAuthorName() {
        if (this.user == null) {
            return "匿名";
        }
        return this.user.getNickname() != null && !this.user.getNickname().isEmpty()
                ? this.user.getNickname()
                : this.user.getUsername();
    }
    // 方便前端 Thymeleaf 讀取分類名稱 (對應 note.type)
    public String getCategory() {
        return this.type != null ? this.type.name() : "其他";
    }

    // ================= 🔖 收藏功能雙向牽線 =================
    @OneToMany(mappedBy = "note", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private List<Bookmark> bookmarks = new ArrayList<>();

    // 取得收藏數量 (給 Thymeleaf 與 SQL 使用)
    public int getBookmarkCount() {
        return this.bookmarks != null ? this.bookmarks.size() : 0;
    }

}
