package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import com.example.demo.service.AchievementService;
import com.example.demo.service.GamerProfileService;
import com.example.demo.service.UserChronicleService;
import com.example.demo.service.UserService;
import com.example.demo.config.AppClock;
import com.example.demo.util.TimeAgoUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Controller
public class AuthController {

    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private FollowRepository followRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookmarkRepository bookmarkRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private UserDailyCounterRepository counterRepository;

    @Autowired
    private GamerProfileService gamerProfileService;

    @Autowired
    private ShopItemRepository shopItemRepository;

    @Autowired
    private QuestRepository questRepository;

    @Autowired
    private UserQuestProgressRepository userQuestProgressRepository;

    // 🏆 注入成就服務
    @Autowired
    private AchievementService achievementService;

    // 📜 注入冒險者編年史服務
    @Autowired
    private UserChronicleService userChronicleService;

    // 登入
    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "target", required = false) String target, Model model) {
        model.addAttribute("target", target);
        return "login";
    }

    @PostMapping("/doLogin")
    public String doLogin(@RequestParam("username") String username,
                          @RequestParam("password") String password,
                          @RequestParam(value = "target", required = false) String target,
                          HttpSession session,
                          Model model) {

        Optional<User> userOptional = userRepository.findByUsername(username);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (passwordEncoder.matches(password, user.getPassword())) {
                session.setAttribute("loginUser", user);

                if (target != null && !target.isEmpty()) {
                    if (target.startsWith("/admin") && !"ADMIN".equals(user.getRole())) {
                        return "redirect:/member";
                    }
                    return "redirect:" + target;
                }
                if ("ADMIN".equals(user.getRole())) {
                    return "redirect:/admin";
                } else if ("USER".equals(user.getRole())) {
                    return "redirect:/member";
                }
            }
        }

        model.addAttribute("error", "帳號或密碼錯誤");
        model.addAttribute("target", target);
        return "login";
    }

    // 註冊
    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/doRegister")
    public String doRegister(@RequestParam("username") String username,
                             @RequestParam("password") String password,
                             @RequestParam("nickname") String nickname,
                             Model model) {

        // 檢查 A：確保欄位沒有空白
        if (username.trim().isEmpty() || password.trim().isEmpty() || nickname.trim().isEmpty()) {
            model.addAttribute("error", "所有欄位皆為必填！");
            return "register";
        }

        // 檢查 B：去資料庫查看看，這個帳號是否已經被註冊過
        Optional<User> existingUser = userRepository.findByUsername(username);
        if (existingUser.isPresent()) {
            model.addAttribute("error", "該帳號已被註冊，請換一個試試看。");
            return "register";
        }

        // 檢查通過，開始建立新使用者
        User newUser = new User();
        newUser.setUsername(username);
        String encodedPassword = passwordEncoder.encode(password);
        newUser.setPassword(encodedPassword);
        newUser.setNickname(nickname);
        newUser.setRole("USER");
        newUser.setCreatedAt(AppClock.now());

        // 2. 🎮 核心冷啟動初始化：給予新玩家初始 RPG 屬性
        newUser.setLevel(1);
        newUser.setExp(0);
        newUser.setCoins(100);

        // 3. 物理落庫，取得具有 ID 的用戶物件
        User savedUser = userRepository.save(newUser);

        // 4. 🛰️ 建立當天的每日任務計數器
        UserDailyCounter initialCounter = new UserDailyCounter();
        initialCounter.setUser(savedUser);
        initialCounter.setNoteCount(0);
        initialCounter.setCommentCount(0);
        initialCounter.setDate(LocalDate.now());
        counterRepository.save(initialCounter);

        List<Quest> dailyQuests = questRepository.findByType(QuestType.DAILY);
        for (Quest quest : dailyQuests) {
            UserQuestProgress userQuestProgress = new UserQuestProgress();
            userQuestProgress.setUser(savedUser);
            userQuestProgress.setQuest(quest);
            userQuestProgress.setLastUpdated(LocalDate.now());
            userQuestProgressRepository.save(userQuestProgress);
        }
        List<Quest> tutorialQuests = questRepository.findByType(QuestType.TUTORIAL);
        for (Quest quest : tutorialQuests) {
            UserQuestProgress userQuestProgress = new UserQuestProgress();
            userQuestProgress.setUser(savedUser);
            userQuestProgress.setQuest(quest);
            userQuestProgress.setLastUpdated(LocalDate.now());
            userQuestProgressRepository.save(userQuestProgress);
        }

        // 🎯 5. 觸發「帳號註冊 / 創建」成就進度判定
        // 請確保資料庫成就表中有 target_type 為 "REGISTER" 的成就
        achievementService.checkAndProgressAchievement(savedUser.getId(), "REGISTER");

        // 📜 6. 寫入冒險者編年史創角歷史事件
        userChronicleService.recordEvent(
                savedUser.getId(),
                "SYSTEM_REGISTER",
                "踏入 NoteShare 冒險者公會",
                "正式註冊成為 NoteShare 社群會員，開啟知識冒險者之旅！",
                "bi-door-open-fill",
                "bg-primary"
        );

        // 註冊成功，帶上成功訊息並導向登入頁面
        model.addAttribute("msg", "帳號註冊成功！請使用新帳號登入。");
        return "login";
    }

    // 📄 負責處理密碼修改提交的 Controller
    @PostMapping("/dashboard/update-password")
    public String doUpdatePassword(
            @RequestParam("oldPassword") String oldPassword,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword,
            HttpSession session,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {

        User sessionUser = (User) session.getAttribute("loginUser");
        if (sessionUser == null) {
            return "redirect:/login";
        }

        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "❌ 兩次輸入的新密碼不一致！");
            return "redirect:/dashboard";
        }

        boolean isSuccess = userService.updatePassword(sessionUser, oldPassword, newPassword);

        if (isSuccess) {
            session.setAttribute("user", sessionUser);
            redirectAttributes.addFlashAttribute("successMessage", "✨ 密碼修改成功！下次登入請使用新密碼。");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "❌ 舊密碼輸入錯誤，密碼變更失敗！");
        }

        return "redirect:/dashboard";
    }

    // 管理員專區
    @GetMapping("/admin")
    public String adminPage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loginUser");

        String currentFrameType = user.getCurrentFrame();
        if (currentFrameType != null) {
            ShopItem equippedFrame = shopItemRepository.findByAssetType(currentFrameType);
            model.addAttribute("equippedFrame", equippedFrame);
        } else {
            model.addAttribute("equippedFrame", null);
        }

        String currentAvatarType = user.getCurrentAvatar();
        if (currentAvatarType != null) {
            ShopItem equippedAvatar = shopItemRepository.findByAssetType(currentAvatarType);
            model.addAttribute("equippedAvatar", equippedAvatar);
        } else {
            model.addAttribute("equippedAvatar", null);
        }

        model.addAttribute("username", user.getNickname() != null ? user.getNickname() : user.getUsername());
        return "admin";
    }

    // 普通會員中心
    @GetMapping("/member")
    public String memberProfile(@RequestParam(value = "username", required = false) String targetUsername,
                                @RequestParam(value = "page", defaultValue = "0") int page,
                                @RequestParam(value = "size", defaultValue = "3") int size,
                                @RequestParam(value = "tab", defaultValue = "published") String tab,
                                jakarta.servlet.http.HttpServletRequest httpRequest,
                                HttpSession session, Model model) {

        User loginUser = (User) session.getAttribute("loginUser");
        model.addAttribute("user", loginUser);
        model.addAttribute("timeUtil", new TimeAgoUtil());
        model.addAttribute("activeTab", tab);

        User targetUser = null;
        boolean isOwner = false;

        if (targetUsername == null || targetUsername.trim().isEmpty()) {
            if (loginUser == null) {
                return "redirect:/login?target=/member";
            }
            targetUser = loginUser;
            isOwner = true;
        } else {
            String cleanUsername = targetUsername.trim();
            if (loginUser != null && loginUser.getUsername().equals(cleanUsername)) {
                targetUser = loginUser;
                isOwner = true;
            } else {
                targetUser = noteRepository.findByVisibilityAndDeletedFalseOrderByCreatedAtDesc("PUBLIC")
                        .stream()
                        .map(Note::getUser)
                        .filter(u -> u.getUsername().equals(cleanUsername))
                        .findFirst().orElse(null);
                isOwner = false;
            }
        }

        if (targetUser == null) {
            System.err.println("⚠️ [主頁路由防禦] 系統嘗試存取不存在的用戶主頁：" + targetUsername);
            return "redirect:/notes";
        }

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);

        org.springframework.data.domain.Page<Note> notePage;
        if (isOwner) {
            notePage = noteRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(targetUser.getId(), pageable);
        } else {
            notePage = noteRepository.findByUserIdAndVisibilityAndDeletedFalseOrderByCreatedAtDesc(targetUser.getId(), "PUBLIC", pageable);
        }

        int followingCount = 0;
        int followersCount = 0;
        boolean isFollowing = false;

        if (followRepository != null) {
            followingCount = followRepository.countByFollowerId(targetUser.getId());
            followersCount = followRepository.countByFollowingId(targetUser.getId());
            if (loginUser != null) {
                isFollowing = followRepository.existsByFollowerIdAndFollowingId(loginUser.getId(), targetUser.getId());
            }
        }

        org.springframework.data.domain.Page<Bookmark> bookmarkPage = bookmarkRepository.findByUserIdAndNoteDeletedFalseOrderByCreatedAtDesc(targetUser.getId(), pageable);
        List<Note> displayBookmarks = new java.util.ArrayList<>();

        if (bookmarkPage != null) {
            for (Bookmark b : bookmarkPage.getContent()) {
                Note originalNote = b.getNote();
                Long noteCreatorId = originalNote.getUser().getId();
                Long currentViewerId = (loginUser != null) ? loginUser.getId() : null;
                boolean isCreatedByMe = noteCreatorId.equals(currentViewerId);

                if ("PRIVATE".equals(originalNote.getVisibility()) && !isCreatedByMe) {
                    Note tombstoneNote = new Note();
                    tombstoneNote.setId(-1L);
                    tombstoneNote.setTitle("🔒 這篇筆記已被原作者收回分享 / 設為私人");
                    tombstoneNote.setType(NoteType.OTHER);
                    tombstoneNote.setContent("由於原作者已將內容隱藏，您先前留存的收藏快照已進入權限管制狀態。");
                    tombstoneNote.setUser(originalNote.getUser());
                    displayBookmarks.add(tombstoneNote);
                } else {
                    displayBookmarks.add(originalNote);
                }
            }
        }

        int totalAchievedLikes = userService.calculateTotalInteractions(targetUser.getId());
        int commentLikes = commentRepository.sumLikesCountByUserId(targetUser.getId());
        int totalViews = noteRepository.sumViewsCountByUserId(targetUser.getId());
        int noteLikes = noteRepository.sumLikesCountByUserId(targetUser.getId());

        model.addAttribute("bookmarks", displayBookmarks);

        if (loginUser != null) {
            String loginFrameType = loginUser.getCurrentFrame();
            if (loginFrameType != null) {
                ShopItem loginFrame = shopItemRepository.findByAssetType(loginFrameType);
                model.addAttribute("equippedFrame", loginFrame);
            } else {
                model.addAttribute("equippedFrame", null);
            }

            String loginAvatarType = loginUser.getCurrentAvatar();
            if (loginAvatarType != null) {
                ShopItem loginAvatar = shopItemRepository.findByAssetType(loginAvatarType);
                model.addAttribute("equippedAvatar", loginAvatar);
            } else {
                model.addAttribute("equippedAvatar", null);
            }
        } else {
            model.addAttribute("equippedFrame", null);
            model.addAttribute("equippedAvatar", null);
        }

        if (targetUser != null) {
            String currentFrameType = targetUser.getCurrentFrame();
            if (currentFrameType != null) {
                ShopItem targetFrame = shopItemRepository.findByAssetType(currentFrameType);
                model.addAttribute("profileFrame", targetFrame);
            } else {
                model.addAttribute("profileFrame", null);
            }

            String currentAvatarType = targetUser.getCurrentAvatar();
            if (currentAvatarType != null) {
                ShopItem targetAvatar = shopItemRepository.findByAssetType(currentAvatarType);
                model.addAttribute("profileAvatar", targetAvatar);
            } else {
                model.addAttribute("profileAvatar", null);
            }
        }

        model.addAttribute("profileUser", targetUser);
        model.addAttribute("notes", notePage.getContent());
        model.addAttribute("bookmarks", displayBookmarks);
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("noteCurrentPage", notePage.getNumber());
        model.addAttribute("noteTotalPages", notePage.getTotalPages());
        model.addAttribute("bookCurrentPage", bookmarkPage.getNumber());
        model.addAttribute("bookTotalPages", bookmarkPage.getTotalPages());

        String requestedWith = httpRequest.getHeader("X-Requested-With");
        if ("Fetch".equals(requestedWith)) {
            if ("bookmarked".equals(tab)) {
                return "member_profile :: bookmarked-list";
            } else {
                return "member_profile :: published-list";
            }
        }

        return "member_profile";
    }

    // 方案 B 核心中繼站：/member/{id}
    @GetMapping("/member/{id}")
    public String showMemberProfileById(@PathVariable("id") Long targetUserId, HttpSession session) {
        User foundUser = noteRepository.findAll().stream()
                .map(Note::getUser)
                .filter(u -> u.getId().equals(targetUserId))
                .findFirst().orElse(null);
        if (foundUser == null) {
            User loginUser = (User) session.getAttribute("loginUser");
            if (loginUser != null && loginUser.getId().equals(targetUserId)) {
                foundUser = loginUser;
            }
        }

        if (foundUser == null) {
            System.err.println("⚠️ [ID轉譯防禦] 找不到 ID 對應的用戶：" + targetUserId);
            return "redirect:/notes";
        }

        return "forward:/member?username=" + foundUser.getUsername();
    }

    // 非同步收藏/取消收藏接口
    @PostMapping("/notes/api/bookmark")
    @org.springframework.web.bind.annotation.ResponseBody
    public java.util.Map<String, Object> toggleBookmark(@RequestParam("noteId") Long noteId, HttpSession session) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        User loginUser = (User) session.getAttribute("loginUser");

        if (loginUser == null) {
            response.put("success", false);
            response.put("message", "請先登入系統後再執行收藏！");
            return response;
        }

        Note note = noteRepository.findById(noteId).orElse(null);
        if (note == null) {
            response.put("success", false);
            response.put("message", "筆記不存在！");
            return response;
        }

        Optional<Bookmark> existing = bookmarkRepository.findByUserIdAndNoteDeletedFalseAndNoteId(loginUser.getId(), noteId);
        if (existing.isPresent()) {
            bookmarkRepository.delete(existing.get());
            response.put("success", true);
            response.put("isBookmarked", false);
        } else {
            Bookmark bookmark = new Bookmark();
            bookmark.setUser(loginUser);
            bookmark.setNote(note);
            bookmarkRepository.save(bookmark);
            response.put("success", true);
            response.put("isBookmarked", true);
        }
        return response;
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}