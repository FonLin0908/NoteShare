package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import com.example.demo.service.AchievementService;
import com.example.demo.service.GamerProfileService;
import com.example.demo.util.StorageUtil;
import com.example.demo.util.TimeAgoUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Controller
public class NoteController {

    @Autowired
    private NoteRepository noteRepository;
    @Autowired
    private NoteLikeRepository noteLikeRepository;
    @Autowired
    private BookmarkRepository bookmarkRepository;
    @Autowired
    private GamerProfileService gamerProfileService;
    @Autowired
    private UserDailyCounterRepository userDailyCounterRepository;
    @Autowired
    private ShopItemRepository shopItemRepository;
    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private AchievementService achievementService;

    // 筆記大廳首頁
    // 支援分類、排序及關鍵字搜尋的筆記列表。
    @GetMapping("/notes")
    public String notesHome(@RequestParam(value = "type", required = false) NoteType type,
                            @RequestParam(value = "sort", defaultValue = "latest") String sort,
                            @RequestParam(value = "keyword", required = false) String keyword,
                            @RequestParam(value = "page", defaultValue = "0") int page, // 接收前端頁碼（預設第 0 頁）
                            Model model, HttpSession session) {

        // 1. 處理會員狀態
        User currentUser = (User) session.getAttribute("loginUser");
        model.addAttribute("user", currentUser);
        model.addAttribute("timeUtil", new TimeAgoUtil());

        //  2. 將前端傳來的 sort 字串精準對齊並組裝成 JPA 鋼鐵排序規格
        Sort jpaSort;
        switch (sort) {
            case "views":
                jpaSort = Sort.by("viewsCount").descending();
                break;
            case "likes":
                jpaSort = Sort.by("likesCount").descending();
                break;
            case "latest":
            default:
                jpaSort = Sort.by("createdAt").descending();
                break;
        }

        // 每頁顯示 9 篇，對應前端的 3x3 版面。
        Pageable pageable = PageRequest.of(page, 12, jpaSort);
        Page<Note> notePage;

        // 依搜尋條件選擇對應的分頁查詢。
        if (keyword != null && !keyword.trim().isEmpty()) {
            String cleanKeyword = keyword.trim();
            if (type != null) {
                // 情況 A-1：有關鍵字 + 有分類
                notePage = noteRepository.findBySearchKeywordAndTypePaged("PUBLIC", type, cleanKeyword, pageable);
            } else {
                // 情況 A-2：有關鍵字 + 全部分類
                notePage = noteRepository.findByVisibilityAndKeywordPaged("PUBLIC", cleanKeyword, pageable);
            }
        } else {
            if (type != null) {
                // 情況 B-1：沒有關鍵字 + 有分類篩選
                notePage = noteRepository.findByVisibilityAndTypeAndDeletedFalse("PUBLIC", type, pageable);
            } else {
                // 🪐 情況 B-2：基本常規大廳展示（全部分類）
                notePage = noteRepository.findByVisibilityAndDeletedFalse("PUBLIC", pageable);
            }
        }

        // 補充列表顯示所需的頭像與頭像框資料。
        if (currentUser != null) {
            String currentFrameType = currentUser.getCurrentFrame();
            if (currentFrameType != null) {
                model.addAttribute("equippedFrame", shopItemRepository.findByAssetType(currentFrameType));
            } else {
                model.addAttribute("equippedFrame", null);
            }

            String currentAvatarType = currentUser.getCurrentAvatar();
            if (currentAvatarType != null) {
                model.addAttribute("equippedAvatar", shopItemRepository.findByAssetType(currentAvatarType));
            } else {
                model.addAttribute("equippedAvatar", null);
            }
        }

        // 🟢 6. 將分頁核心數據與狀態全數回傳給前端大廳
        model.addAttribute("notes", notePage.getContent());          // 撈出當前頁面的筆記清單 (List<Note>)
        model.addAttribute("currentPage", page);                      // 當前頁碼（供按鈕高亮）
        model.addAttribute("totalPages", notePage.getTotalPages());   // 總頁數（供迴圈渲染按鈕群）
        model.addAttribute("hasNext", notePage.hasNext());            // 是否有下一頁
        model.addAttribute("hasPrevious", notePage.hasPrevious());    // 是否有上一頁

        model.addAttribute("currentType", type != null ? type.name() : "");
        model.addAttribute("currentSort", sort);
        model.addAttribute("currentKeyword", keyword);
        model.addAttribute("allNoteTypes", NoteType.values());

        return "notes_home";
    }
    // 查看指定筆記的詳細內容
    @GetMapping("/notes/view")
    public String viewNote(@RequestParam("id") Long id,@RequestParam(value = "token", required = false) String token, Model model, HttpSession session) {

        // 1. 處理右上角會員頭像狀態（保持不變）
        User currentUser = (User) session.getAttribute("loginUser");
        model.addAttribute("user", currentUser);

        // 2. 去 Supabase 尋找這篇筆記
        java.util.Optional<Note> noteOptional = noteRepository.findById(id);

        if (noteOptional.isPresent()) {
            Note note = noteOptional.get();
            if(!note.getVisibility().equals("PUBLIC") && !(note.getVisibility().equals("PRIVATE") && note.getShareToken().equals(token))) {
                if(currentUser == null) {
                    return "redirect:/notes";
                }
                else if(!note.getUser().getUsername().equals(currentUser.getUsername())) {
                    return "redirect:/notes";
                }
            }
            // 【核心新增】點閱次數自動加 1，並同步更新回 Supabase 雲端
            note.setViewsCount(note.getViewsCount() + 1);
            noteRepository.save(note); // 儲存更新後的點閱數

            //  【全新防刷判定】預設沒點過讚。如果已登入，去資料庫翻看有沒有點讚紀錄
            boolean hasLiked = false;
            if (currentUser != null) {
                hasLiked = noteLikeRepository.existsByUserIdAndNoteId(currentUser.getId(), note.getId());
            }
            // 僅傳送收藏筆記 ID，供前端標示收藏狀態。
            java.util.Set<Long> bookmarkedNoteIds = new java.util.HashSet<>();

            if (currentUser != null) {
                // 去關聯庫抓出此人名下的所有收藏，並把裡面的 Note ID 全部抽出來
                List<Bookmark> myBookmarks = bookmarkRepository.findByUserIdAndNoteDeletedFalseOrderByCreatedAtDesc(currentUser.getId());
                if (myBookmarks != null) {
                    for (Bookmark b : myBookmarks) {
                        if (b.getNote() != null) {
                            bookmarkedNoteIds.add(b.getNote().getId());
                        }
                    }
                }
            }

            // 把這個由 ID 組成的萬能密碼箱丟給前端 Thymeleaf
            // 裡面可能長這樣：[3, 7, 12] 代表這三篇我收藏過
            model.addAttribute("bookmarkedIds", bookmarkedNoteIds);
            model.addAttribute("user", currentUser);

            if(currentUser != null) {
                String currentFrameType = currentUser.getCurrentFrame(); // 比如拿到 "FRAME_LAVA"

                //全部外觀屬性(頭相框)
                if (currentFrameType != null) {
                    // 去資料庫把這件商品的完整屬性（包含 style_value）撈出來
                    ShopItem equippedFrame = shopItemRepository.findByAssetType(currentFrameType);
                    model.addAttribute("equippedFrame", equippedFrame);
                } else {
                    model.addAttribute("equippedFrame", null);
                }

                String currentAvatarType = currentUser.getCurrentAvatar(); // 比如拿到 "AVATAR_CAT"

                if (currentAvatarType != null) {
                    // 透過我們剛才在 Repository 焊好的黃金方法，直接撈出頭像道具的完整資料
                    ShopItem equippedAvatar = shopItemRepository.findByAssetType(currentAvatarType);
                    model.addAttribute("equippedAvatar", equippedAvatar);
                } else {
                    model.addAttribute("equippedAvatar", null);
                }

            }

            if (note != null && note.getUser() != null) {
                // 解除 Hibernate Proxy，以便填入僅供畫面顯示的暫存欄位。
                User realUser = (User) org.hibernate.Hibernate.unproxy(note.getUser());

                // 重新查詢作者目前裝備的頭像與頭像框。
                String authorFrameType = realUser.getCurrentFrame();
                if (authorFrameType != null) {
                    ShopItem authorFrame = shopItemRepository.findByAssetType(authorFrameType);
                    realUser.setCurrentFrame(authorFrame.getStyleValue()); // ✅ 實體真身可以安全 set 暫時欄位
                }

                String authorAvatarType = realUser.getCurrentAvatar();
                if (authorAvatarType != null) {
                    ShopItem authorAvatar = shopItemRepository.findByAssetType(authorAvatarType);
                    realUser.setCurrentAvatar(authorAvatar.getStyleValue()); // ✅ 實體真身安全 set 大頭貼
                }

                // 3. 把剝離假身、焊好發光神裝的完全體 realUser 放回 note 裡面
                note.setUser(realUser);
            }

            // 將這篇特定的筆記傳給前端
            model.addAttribute("note", note);
            model.addAttribute("hasLiked", hasLiked); // 傳給前端，決定愛心是一開始就是紅的還是空心的
            return "note_detail"; // 尋找 templates/note_detail.html

        }

        // 如果輸入了不存在的 ID，直接被彈回大廳
        return "redirect:/notes";
    }
    // 顯示建立筆記表單；未登入時導回登入頁。
    @GetMapping("/notes/create")
    public String createNotePage(HttpSession session, Model model) {

        // 看看 Session 裡面有沒有登入者物件
        User currentUser = (User) session.getAttribute("loginUser");

        // 尋找 NoteController 裡的 createNotePage 方法
        if (currentUser == null) {
            // 沒登入？強制彈回登入頁，並順便把建立頁面的網址當成 target 掛在後面
            return "redirect:/login?target=/notes/create";
        }

        if(currentUser != null) {
            String currentFrameType = currentUser.getCurrentFrame(); // 比如拿到 "FRAME_LAVA"

            //全部外觀屬性(頭相框)
            if (currentFrameType != null) {
                // 去資料庫把這件商品的完整屬性（包含 style_value）撈出來
                ShopItem equippedFrame = shopItemRepository.findByAssetType(currentFrameType);
                model.addAttribute("equippedFrame", equippedFrame);
            } else {
                model.addAttribute("equippedFrame", null);
            }

            String currentAvatarType = currentUser.getCurrentAvatar(); // 比如拿到 "AVATAR_CAT"

            if (currentAvatarType != null) {
                // 透過我們剛才在 Repository 焊好的黃金方法，直接撈出頭像道具的完整資料
                ShopItem equippedAvatar = shopItemRepository.findByAssetType(currentAvatarType);
                model.addAttribute("equippedAvatar", equippedAvatar);
            } else {
                model.addAttribute("equippedAvatar", null);
            }

        }

        // 有登入？順利放行進入建立頁面
        model.addAttribute("user", currentUser);
        return "note_create"; // 尋找 templates/note_create.html
    }
    // 4. 💾 【核心功能】接收表單數據並寫入 Supabase
    @PostMapping("/notes/doCreate")
    public String doCreateNote(
            @RequestParam("title") String title,
            @RequestParam("type") NoteType type,
            @RequestParam(value = "tags", defaultValue = "") String tags,
            @RequestParam("content") String content,
            @RequestParam("visibility") String visibility,

            // 接收封面與內文圖片。
            @RequestParam(value = "coverImageFile", required = false) MultipartFile coverImageFile,
            @RequestParam(value = "attachmentUrl", required = false) String attachmentUrl,
            @RequestParam(value = "attachmentFileName", required = false) String attachmentFileName,
            @RequestParam(value = "imageFiles", required = false) MultipartFile[] imageFiles,

            @RequestParam(value = "youtubeVideoId", required = false) String youtubeVideoUrl,
            HttpSession session, Model model) {

        User currentUser = (User) session.getAttribute("loginUser");
        if (currentUser == null) {
            model.addAttribute("error", "登入逾時，請重新登入後再發布筆記！");
            return "login";
        }

        Note note = new Note();
        note.setTitle(title);
        note.setType(type);
        note.setTags(tags);
        note.setContent(content);
        note.setVisibility(visibility);
        note.setUser(currentUser);
        note.setViewsCount(0);
        note.setLikesCount(0);

        // 上傳封面圖。
        if (coverImageFile != null && !coverImageFile.isEmpty()) {
            String uploadedCoverUrl = StorageUtil.uploadFile(coverImageFile, "covers");
            note.setCoverImageUrl(uploadedCoverUrl); // 塞入 Supabase 傳回來的永久網址
        } else {
            note.setCoverImageUrl(null); // 沒選的話給 null，前端會自動用預設科技圖
        }

        // 附件目前以外部連結形式儲存。
        if (attachmentUrl != null && !attachmentUrl.trim().isEmpty()) {
            note.setAttachmentUrl(attachmentUrl);

            // 未提供顯示名稱時使用預設文字。
            if (attachmentFileName == null || attachmentFileName.trim().isEmpty()) {
                note.setAttachmentFileName("點我前往隨附附件連結");
            } else {
                note.setAttachmentFileName(attachmentFileName);
            }
        } else {
            note.setAttachmentUrl(null);
            note.setAttachmentFileName(null);
        }

        // 將支援的 YouTube 網址正規化為影片 ID。
        String extractedId = com.example.demo.util.YouTubeParamUtil.extractId(youtubeVideoUrl);
        note.setYoutubeVideoId(extractedId);

        // 先儲存筆記以取得內文圖片關聯所需的 ID。
        Note savedNote = noteRepository.save(note);

        // 儲存內文圖片並維護 Note 與 NoteImage 的雙向關聯。
        if (imageFiles != null && imageFiles.length > 0) {

            // 沿用 Hibernate 管理的集合；只有尚未初始化時才建立新集合。
            if (savedNote.getImages() == null) {
                savedNote.setImages(new ArrayList<>());
            }

            // 迴圈批次處理實體檔案
            for (MultipartFile imgFile : imageFiles) {
                if (imgFile != null && !imgFile.isEmpty()) {
                    // 將插圖寫入本地硬碟（或 Supabase）
                    String uploadedImageUrl = StorageUtil.uploadFile(imgFile, "images");

                    if (uploadedImageUrl != null) {
                        NoteImage noteImage = new NoteImage();
                        noteImage.setImageUrl(uploadedImageUrl);
                        noteImage.setNote(savedNote); // 🔗 綁定父子關聯

                        // 加入既有集合，避免替換 Hibernate 管理中的關聯集合。
                        savedNote.getImages().add(noteImage);
                    }
                }
            }

            // 只要箱子裡不為空，就更新存檔
            if (!savedNote.getImages().isEmpty()) {
                noteRepository.save(savedNote);
                System.out.println("📸 [插圖關聯] 成功將 " + savedNote.getImages().size() + " 張插圖平滑添加進 JPA 託管容器！");
            }
        }
        UserDailyCounter dailyCount = userDailyCounterRepository.findByUser(currentUser).orElse(null);
        if (dailyCount != null) {
            dailyCount.setNoteCount(dailyCount.getNoteCount() + 1);
            userDailyCounterRepository.save(dailyCount);
        }

        System.out.println("🎉 [系統提示] 使用者 " + currentUser.getUsername() + " 成功發布了筆記！");

        achievementService.checkAndProgressAchievement(currentUser.getId(), "NOTE_COUNT");
        gamerProfileService.handleUserAction(currentUser.getUsername(), UserAction.NOTE);
        return "redirect:/notes";
    }

    // 刪除筆記前驗證擁有者，並清理關聯的圖片檔案。
    @GetMapping("/notes/delete/{id}")
    @Transactional
    public String deleteNote(@org.springframework.web.bind.annotation.PathVariable("id") Long id, HttpSession session) {

        // 未登入時不執行刪除。
        User currentUser = (User) session.getAttribute("loginUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        // 去資料庫搜尋是否有這篇筆記
        java.util.Optional<Note> noteOptional = noteRepository.findById(id);
        if (noteOptional.isEmpty()) {
            // 查無此筆記？安全退回大廳
            return "redirect:/notes";
        }

        Note note = noteOptional.get();

        // 只有筆記作者可以刪除內容。
        if (!note.getUser().getId().equals(currentUser.getId())) {
            System.err.println("⚠️ [安全警報] 使用者【" + currentUser.getUsername() + "】試圖繞過前台越權刪除他人筆記【ID: " + id + "】！已成功攔截。");
            return "redirect:/notes";
        }
        // 刪除命令
        /*if (note.getCoverImageUrl() != null) {
            StorageUtil.deleteLocalImageFile(note.getCoverImageUrl());
        }

        // 正式從 Repository 發動刪除
        noteRepository.delete(note);*/

        // 改成軟刪除
        note.setVisibility("PRIVATE");
        note.setDeleted(true);
        commentRepository.softDeleteAllByNoteId(note.getId());
        noteRepository.save(note);

        System.out.println("🗑️ [系統提示] 擁有者【" + currentUser.getUsername() + "】已正式將筆記【" + note.getTitle() + "】從資料庫永久下架。");

        // 功成身退，平滑跳轉回筆記大廳首頁
        return "redirect:/notes";
    }

    // 🎨 【編輯頁面】抓出舊資料並開啟編輯表單
    @GetMapping("/notes/edit/{id}")
    public String editNotePage(@PathVariable("id") Long id, HttpSession session, Model model) {
        // 1. 安全檢查：沒登入不能進入
        User currentUser = (User) session.getAttribute("loginUser");
        if (currentUser == null) return "redirect:/login";

        // 2. 尋找目標筆記
        java.util.Optional<Note> noteOptional = noteRepository.findById(id);
        if (noteOptional.isPresent()) {
            Note note = noteOptional.get();

            // 3. 安全防線：防止路人直接手改網址修改別人的筆記
            if (!note.getUser().getId().equals(currentUser.getId())) {
                return "redirect:/notes"; // 不是本人，彈回大廳
            }

            if(currentUser != null) {
                String currentFrameType = currentUser.getCurrentFrame(); // 比如拿到 "FRAME_LAVA"

                //全部外觀屬性(頭相框)
                if (currentFrameType != null) {
                    // 去資料庫把這件商品的完整屬性（包含 style_value）撈出來
                    ShopItem equippedFrame = shopItemRepository.findByAssetType(currentFrameType);
                    model.addAttribute("equippedFrame", equippedFrame);
                } else {
                    model.addAttribute("equippedFrame", null);
                }

                String currentAvatarType = currentUser.getCurrentAvatar(); // 比如拿到 "AVATAR_CAT"

                if (currentAvatarType != null) {
                    // 透過我們剛才在 Repository 焊好的黃金方法，直接撈出頭像道具的完整資料
                    ShopItem equippedAvatar = shopItemRepository.findByAssetType(currentAvatarType);
                    model.addAttribute("equippedAvatar", equippedAvatar);
                } else {
                    model.addAttribute("equippedAvatar", null);
                }

            }
            model.addAttribute("note", note); // 將舊資料打包給前端
            model.addAttribute("user", currentUser);
            return "note_edit"; // 前往編輯網頁
        }
        return "redirect:/notes";
    }

    // 💾 【執行更新】接收修改後的數據並覆蓋回資料庫
    @PostMapping("/notes/doUpdate")
    public String doUpdateNote(
            @RequestParam("id") Long id,
            @RequestParam("title") String title,
            @RequestParam("type") NoteType type,
            @RequestParam(value = "tags", defaultValue = "") String tags,
            @RequestParam("content") String content,
            @RequestParam("visibility") String visibility,
            @RequestParam(value = "youtubeVideoId", required = false) String youtubeVideoUrl,
            @RequestParam(value = "attachmentUrl", required = false) String attachmentUrl,
            @RequestParam(value = "attachmentFileName", required = false) String attachmentFileName,
            // 可選擇替換封面與全部內文圖片。
            @RequestParam(value = "coverImageFile", required = false) org.springframework.web.multipart.MultipartFile coverImageFile,
            @RequestParam(value = "imageFiles", required = false) org.springframework.web.multipart.MultipartFile[] imageFiles,
            HttpSession session) throws java.io.IOException {

        // 1. 身分安全性驗證
        User currentUser = (User) session.getAttribute("loginUser");
        Note note = noteRepository.findById(id).orElse(null);

        if (currentUser == null || note == null || !note.getUser().getId().equals(currentUser.getId())) {
            return "redirect:/notes";
        }

        // 2. 更新基礎文字欄位
        note.setTitle(title);
        note.setType(type);
        note.setTags(tags);
        note.setContent(content);
        note.setVisibility(visibility);
        note.setAttachmentUrl(attachmentUrl);
        note.setAttachmentFileName(attachmentFileName);

        // 處理 YouTube 網址轉換
        String extractedId = com.example.demo.util.YouTubeParamUtil.extractId(youtubeVideoUrl);
        note.setYoutubeVideoId(extractedId);

        // ================= 📸 3. 處理封面圖更換 =================
        if (coverImageFile != null && !coverImageFile.isEmpty()) {
            StorageUtil.deleteLocalImageFile(note.getCoverImageUrl());
            // 編輯時將新封面儲存至專案的 uploads/covers 資料夾。
            String uploadDir = System.getProperty("user.dir") + "/uploads/covers/";
            java.io.File dir = new java.io.File(uploadDir);
            if (!dir.exists()) dir.mkdirs();

            String originalFilename = coverImageFile.getOriginalFilename();
            String ext = originalFilename.substring(originalFilename.lastIndexOf("."));
            String newFilename = java.util.UUID.randomUUID().toString() + ext;

            java.io.File destFile = new java.io.File(uploadDir + newFilename);
            coverImageFile.transferTo(destFile);

            // 覆蓋舊的封面圖 URL 路徑
            note.setCoverImageUrl("/uploads/covers/" + newFilename);
            System.out.println("🖼️ [封面更新] 已成功更換新封面：" + newFilename);
        }

        // ================= 📸 4. 處理多張插圖完全覆蓋 =================
        // 只有當使用者「真的有選擇新插圖檔案」時，才進行覆蓋更新
        if (imageFiles != null && imageFiles.length > 0 && !imageFiles[0].isEmpty()) {
            // A. 先清空原本這篇筆記在資料庫裡的舊插圖關聯 (如果你的插圖是有獨立 Repository 或是 Cascade，可在此做清理)
            for(NoteImage i : note.getImages()){
                StorageUtil.deleteLocalImageFile(i.getImageUrl());
            }
            // 假設你的 Note 實體中與 NoteImage 是常規關聯，我們先清除原本的 List
            if (note.getImages() != null) {
                note.getImages().clear();
            } else {
                note.setImages(new java.util.ArrayList<>());
            }

            // B. 循環上傳每一張新選的圖片，並塞入筆記中
            String uploadDir = System.getProperty("user.dir") + "/uploads/images/";
            for (org.springframework.web.multipart.MultipartFile file : imageFiles) {
                if (!file.isEmpty()) {
                    String originalFilename = file.getOriginalFilename();
                    String ext = originalFilename.substring(originalFilename.lastIndexOf("."));
                    String newFilename = java.util.UUID.randomUUID().toString() + ext;

                    java.io.File destFile = new java.io.File(uploadDir + newFilename);
                    file.transferTo(destFile);

                    // 建立新的圖片物件 (請根據你實際的 NoteImage 類別名稱與欄位調整)
                    NoteImage noteImage = new NoteImage();
                    noteImage.setImageUrl("/uploads/images/" + newFilename);
                    noteImage.setNote(note); // 綁定外鍵關係

                    note.getImages().add(noteImage);
                }
            }
            System.out.println("📸 [插圖更新] 已成功完全覆蓋並增補新插圖，共計：" + note.getImages().size() + " 張");
        }

        // 5. 儲存至資料庫
        noteRepository.save(note);

        // 帶使用者回到詳細頁見證成果
        return "redirect:/notes/view?id=" + id;
    }

    //按讚與取消按讚
    @PostMapping("/notes/like")
    @ResponseBody
    public java.util.Map<String, Object> toggleLike(@RequestParam("id") Long id, HttpSession session) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();

        User currentUser = (User) session.getAttribute("loginUser");
        if (currentUser == null) {
            response.put("success", false);
            response.put("message", "請先登入帳號再進行推薦！");
            return response;
        }

        java.util.Optional<Note> noteOptional = noteRepository.findById(id);
        if (noteOptional.isPresent()) {
            Note note = noteOptional.get();

            // 🔍 檢查資料庫有沒有這一筆點讚紀錄
            java.util.Optional<com.example.demo.model.NoteLike> existingLike =
                    noteLikeRepository.findByUserIdAndNoteId(currentUser.getId(), note.getId());

            int newLikesCount = note.getLikesCount();
            boolean isLikedNow;

            if (existingLike.isPresent()) {
                // ❌ 情況 A：以前點過讚 ➡️ 這次點擊代表「取消點讚」
                noteLikeRepository.delete(existingLike.get()); // 刪除點讚紀錄
                newLikesCount = Math.max(0, newLikesCount - 1); // 總讚數 -1 (防負數)
                isLikedNow = false;
            } else {
                // ❤️ 情況 B：以前沒點過 ➡️ 這次點擊代表「給予點讚」
                com.example.demo.model.NoteLike newLike = new com.example.demo.model.NoteLike();
                newLike.setUser(currentUser);
                newLike.setNote(note);
                noteLikeRepository.save(newLike); // 寫入新紀錄
                gamerProfileService.handleUserAction(currentUser.getUsername(), UserAction.LIKE);


                newLikesCount = newLikesCount + 1; // 總讚數 +1
                isLikedNow = true;
            }

            // 將最新的總讚數儲存回筆記本體
            note.setLikesCount(newLikesCount);
            noteRepository.save(note);

            // 回傳給前端：除了最新數字，還要告訴前端現在是「點讚中(true)」還是「空心(false)」
            response.put("success", true);
            response.put("newLikesCount", newLikesCount);
            response.put("isLiked", isLikedNow);

            achievementService.checkAndProgressAchievementNote(note.getId(), "NOTE_LIKE_ONCE");

            return response;
        }

        response.put("success", false);
        return response;
    }

    @Autowired
    private com.example.demo.repository.FollowRepository followRepository;
    @Autowired
    private com.example.demo.repository.UserRepository userRepository; // 假設你用這個來找對象

    // 👥 【非同步追蹤與取消追蹤交換機】
    @PostMapping("/member/follow")
    @org.springframework.web.bind.annotation.ResponseBody
    public java.util.Map<String, Object> toggleFollow(@RequestParam("username") String targetUsername, HttpSession session) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();

        // 1. 門禁檢查
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            response.put("success", false);
            response.put("message", "請先登入帳號再執行追蹤功能！");
            return response;
        }

        // 2. 尋找目標創作者 (假設透過 username 找，此處與你現有 UserRepository 配對)
        // 💡 實際部署請確保 userRepository 有 findByUsername 方法，以下為標準邏輯：
        User targetUser = noteRepository.findByVisibilityAndDeletedFalseOrderByCreatedAtDesc("PUBLIC")
                .stream()
                .map(Note::getUser)
                .filter(u -> u.getUsername().equals(targetUsername))
                .findFirst().orElse(null); // 此行安全防錯，可依你專案內實際的 user 查詢方式替換

        if (targetUser == null) {
            response.put("success", false);
            response.put("message", "找不到該目標用戶！");
            return response;
        }

        // 不允許使用者追蹤自己。
        if (loginUser.getId().equals(targetUser.getId())) {
            response.put("success", false);
            response.put("message", "您不能追蹤您自己！");
            return response;
        }

        // 3. 核心動態切換邏輯
        java.util.Optional<Follow> existingFollow = followRepository.findByFollowerIdAndFollowingId(loginUser.getId(), targetUser.getId());
        boolean isFollowingNow;

        if (existingFollow.isPresent()) {
            // ❌ 情況 A：以前追蹤過 ➡️ 執行【取消追蹤】
            followRepository.delete(existingFollow.get());
            isFollowingNow = false;
            System.out.println("👥 [社群動態] 使用者【" + loginUser.getUsername() + "】取消追蹤了【" + targetUsername + "】");
        } else {
            // ➕ 情況 B：以前沒追蹤 ➡️ 執行【給予追蹤】
            Follow newFollow = new Follow();
            newFollow.setFollower(loginUser);
            newFollow.setFollowing(targetUser);
            followRepository.save(newFollow);
            isFollowingNow = true;
            System.out.println("👥 [社群動態] 使用者【" + loginUser.getUsername() + "】成功追蹤了【" + targetUsername + "】");
        }

        // 4. 計算該目標用戶最新的粉絲總數，讓前端即時跳數字
        int newFollowersCount = followRepository.countByFollowingId(targetUser.getId());

        response.put("success", true);
        // 回傳給前端，讓按鈕知道要顯示「取消追蹤」還是「追蹤作者」
        response.put("isFollowing", isFollowingNow);
        response.put("newFollowersCount", newFollowersCount);
        return response;
    }

    // 🎛️ 1. 【非同步開關】開啟、關閉、或重置半公開加密 Token
    // 更新筆記的公開、私人或連結分享權限。
    @PostMapping("/notes/api/updateVisibility")
    @org.springframework.web.bind.annotation.ResponseBody
    public java.util.Map<String, Object> updateNoteVisibility(@RequestParam("id") Long noteId,
                                                              @RequestParam("visibility") String targetVisibility,
                                                              HttpSession session) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        User currentUser = (User) session.getAttribute("loginUser");
        Note note = noteRepository.findById(noteId).orElse(null);

        if (currentUser == null || note == null || !note.getUser().getId().equals(currentUser.getId())) {
            response.put("success", false);
            response.put("message", "權限不足或物件不存在！");
            return response;
        }

        // ⚙️ 根據前端傳入的暗號，精準執行三軌切割
        if ("PUBLIC".equals(targetVisibility)) {
            // 🌐 完全公開：將 visibility 改為 PUBLIC，同時將半公開 Token 抹除
            note.setVisibility("PUBLIC");
            note.setShareToken(null);
        }
        else if ("SHARED".equals(targetVisibility)) {
            // 🔗 半公開：如果本來沒有 Token，就生成一個；並確保 visibility 為 PRIVATE（不登大廳）
            note.setVisibility("PRIVATE");
            if (note.getShareToken() == null || note.getShareToken().trim().isEmpty()) {
                note.setShareToken(java.util.UUID.randomUUID().toString());
            }
        }
        else if ("PRIVATE".equals(targetVisibility)) {
            // 🔒 僅限自己：將狀態歸零，Token 切斷抹除
            note.setVisibility("PRIVATE");
            note.setShareToken(null);
        }

        noteRepository.save(note);
        response.put("success", true);
        return response;
    }

    // 🌐 2. 【路人專屬分享通道】像素級重定向回詳細頁面
    @GetMapping("/notes/share/{token}")
    public String accessSharedNote(@PathVariable("token") String token, HttpSession session, Model model) {
        // 尋找這個 Token 對應的筆記
        java.util.Optional<Note> noteOptional = noteRepository.findByShareToken(token);

        if (noteOptional.isPresent()) {
            Note note = noteOptional.get();
            // 權限驗證通過後，轉交既有的筆記詳細頁處理流程。
            // 這樣畫面就會長得一模一樣，不用重寫任何視圖！
            return "forward:/notes/view?id=" + note.getId() + "&token=" + token;
        }

        // 🚨 3. 收藏同步崩潰防線：如果輸入了 Token 卻查無此筆記（代表作者關閉了分享）
        // 為了讓前端能彈出「這篇變成不公開」的溫馨提示，我們帶一個 flag 轉發到大廳或給予通知
        model.addAttribute("sharedAlert", "PRIVATE_LOCK");
        return "forward:/notes"; // 帶著暗號回到筆記大廳，由大廳彈出精美警告視窗！
    }
}
