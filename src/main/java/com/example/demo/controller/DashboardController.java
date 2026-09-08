package com.example.demo.controller;

import com.example.demo.dto.AchievementProgressDTO;
import com.example.demo.model.*;
import com.example.demo.repository.*;
import com.example.demo.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    @Autowired
    private NoteRepository noteRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private UserDailyCounterRepository userDailyCounterRepository;
    @Autowired
    private UserQuestProgressRepository userQuestProgressRepository;
    @Autowired
    private GamerProfileService gamerProfileService;
    @Autowired
    private UserService userService;
    @Autowired
    private ShopService shopService;
    @Autowired
    private ShopItemRepository shopItemRepository;
    @Autowired
    private AchievementService achievementService;
    @Autowired
    private UserChronicleService chronicleService;

    @GetMapping
    public String index(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loginUser");
        // 確保使用者已登入
        if (user == null) {
            return "redirect:/login?target=/dashboard";
        }
        User freshUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("找不到該冒險者"));
        session.setAttribute("loginUser", freshUser);
        model.addAttribute("user", freshUser);

        // 計算用戶總發表筆記數量與留言數量
        int totalNotesCount = noteRepository.countByUserId(freshUser.getId());
        int totalCommentsCount = (int) commentRepository.countByUserId(freshUser.getId());

        model.addAttribute("totalNotesCount", totalNotesCount);
        model.addAttribute("totalCommentsCount", totalCommentsCount);

        // 計算筆記與留言所獲得的按心數與總共加起來的數量
        int noteLikes = noteRepository.sumLikesCountByUserId(freshUser.getId());
        int commentLikes = commentRepository.sumLikesCountByUserId(freshUser.getId());
        int totalLikes = noteLikes + commentLikes;
        int totalViews = noteRepository.sumViewsCountByUserId(freshUser.getId());

        model.addAttribute("noteLikes", noteLikes);
        model.addAttribute("commentLikes", commentLikes);
        model.addAttribute("totalLikes", totalLikes);
        model.addAttribute("totalViews", totalViews);

        // 近七日真實動態統計資料供應鏈
        LocalDate today = LocalDate.now();
        LocalDateTime startDateTime = today.minusDays(6).atStartOfDay(); // 七天前的 00:00:00

        // 從資料庫撈出非零的統計原始數據
        List<Object[]> noteRawData = noteRepository.countWeeklyNotes(freshUser.getId(), startDateTime);
        List<Object[]> commentRawData = commentRepository.countWeeklyComments(freshUser.getId(), startDateTime);

        // 利用 Map 建立「日期 -> 數量」的對齊工具，方便防禦與填充 0
        Map<String, Integer> noteMap = new HashMap<>();
        Map<String, Integer> commentMap = new HashMap<>();

        for (Object[] row : noteRawData) {
            noteMap.put(row[0].toString(), ((Long) row[1]).intValue());
        }
        for (Object[] row : commentRawData) {
            commentMap.put(row[0].toString(), ((Long) row[1]).intValue());
        }

        // 建立前端 Chart.js 所需的三個標準陣列
        String[] dates = new String[7];
        int[] weeklyNotes = new int[7];
        int[] weeklyComments = new int[7];

        DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("MM/dd");
        DateTimeFormatter dbFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // 核心對齊迴圈：確保過去 7 天每天都有專屬坑位，沒資料就自動補 0
        for (int i = 6; i >= 0; i--) {
            LocalDate targetDate = today.minusDays(i);
            int index = 6 - i;

            dates[index] = targetDate.format(labelFormatter);
            String dbKey = targetDate.format(dbFormatter);

            weeklyNotes[index] = noteMap.getOrDefault(dbKey, 0);
            weeklyComments[index] = commentMap.getOrDefault(dbKey, 0);
        }

        model.addAttribute("chartLabels", dates);
        model.addAttribute("weeklyNotes", weeklyNotes);
        model.addAttribute("weeklyComments", weeklyComments);

        // RPG
        long maxExp = gamerProfileService.getRequiredTotalExpForLevel(freshUser.getLevel() + 1);
        double expPercentage = (freshUser.getExp() * 100.0) / maxExp;
        model.addAttribute("expPercentage", expPercentage);
        model.addAttribute("maxExp", maxExp);

        // 每日重置計數
        UserDailyCounter userDailyCounter = userDailyCounterRepository.findByUser(freshUser).orElse(null);
        int dailyNote = userDailyCounter == null ? 0 : userDailyCounter.getNoteCount();
        int dailyComment = userDailyCounter == null ? 0 : userDailyCounter.getCommentCount();

        model.addAttribute("dailyNote", dailyNote);
        model.addAttribute("dailyComment", dailyComment);

        // 🛒 常駐商城貨架
        List<ShopItem> shopProducts = shopService.getRegularShopItems();
        model.addAttribute("shopProducts", shopProducts);

        // 👕 撈出玩家擁有的所有資產清單代碼
        List<String> rawUnlocked = shopService.getUnlockedAssetStrings(freshUser.getId());
        final List<String> finalUnlocked = (rawUnlocked != null) ? rawUnlocked : List.of();
        model.addAttribute("unlockedAssets", finalUnlocked);

        // 🎯 核心修正：直接透過 In 查詢撈出所有已解鎖的外觀實體（支援成就限定非賣品）
        List<ShopItem> unlockedProducts = finalUnlocked.isEmpty()
                ? List.of()
                : shopItemRepository.findByAssetTypeIn(finalUnlocked);
        model.addAttribute("unlockedProducts", unlockedProducts);

        // 裝備頭像框
        String currentFrameType = freshUser.getCurrentFrame();
        if (currentFrameType != null) {
            ShopItem equippedFrame = shopItemRepository.findByAssetType(currentFrameType);
            model.addAttribute("equippedFrame", equippedFrame);
        } else {
            model.addAttribute("equippedFrame", null);
        }

        // 裝備頭像
        String currentAvatarType = freshUser.getCurrentAvatar();
        if (currentAvatarType != null) {
            ShopItem equippedAvatar = shopItemRepository.findByAssetType(currentAvatarType);
            model.addAttribute("equippedAvatar", equippedAvatar);
        } else {
            model.addAttribute("equippedAvatar", null);
        }

        // 玩家任務進度
        List<UserQuestProgress> progresses = userQuestProgressRepository.findAllByUserCustomOrder(freshUser);

        // 切換任務已讀
        boolean needSave = false;
        for (UserQuestProgress prog : progresses) {
            if (!prog.isViewed()) {
                prog.setViewed(true);
                userQuestProgressRepository.save(prog);
                needSave = true;
            }
        }
        if (needSave) {
            progresses = userQuestProgressRepository.findAllByUserCustomOrder(freshUser);
        }

        model.addAttribute("questProgresses", progresses);
        model.addAttribute("activePage", "home");

        // 成就進度列表
        List<AchievementProgressDTO> achievements = achievementService.getUserAchievementList(freshUser.getId());
        model.addAttribute("achievements", achievements);

        // 編年史紀錄
        List<UserChronicle> chronicles = chronicleService.getUserChronicles(freshUser.getId());
        model.addAttribute("chronicles", chronicles);

        return "dashboard/layout";
    }

    @GetMapping("/settings")
    public String settingsPage(HttpSession session, Model model) {
        if (session.getAttribute("loginUser") == null) {
            return "redirect:/login?target=/dashboard/settings";
        }
        model.addAttribute("activePage", "settings");
        return "dashboard/layout";
    }

    @GetMapping("/refresh-wardrobe")
    public String refreshWardrobeHtml(HttpSession session, Model model) {
        User sessionUser = (User) session.getAttribute("loginUser");
        if (sessionUser == null) {
            return "auth/login";
        }

        // 1. 強制去資料庫抓最新 User 物件
        User freshUser = userRepository.findById(sessionUser.getId())
                .orElseThrow(() -> new RuntimeException("找不到該冒險者"));
        session.setAttribute("loginUser", freshUser);

        // 2. 撈出已解鎖的所有外觀代碼
        List<String> rawUnlocked = shopService.getUnlockedAssetStrings(freshUser.getId());
        List<String> finalUnlocked = (rawUnlocked != null) ? rawUnlocked : List.of();
        model.addAttribute("unlockedAssets", finalUnlocked);

        // 3. 🎯 核心修正：直接透過 In 批次查詢撈取所有已解鎖的外觀（含成就專屬道具）
        List<ShopItem> unlockedProducts = finalUnlocked.isEmpty()
                ? List.of()
                : shopItemRepository.findByAssetTypeIn(finalUnlocked);

        // 4. 塞入 Model
        model.addAttribute("user", freshUser);
        model.addAttribute("unlockedProducts", unlockedProducts);

        String currentFrameType = freshUser.getCurrentFrame();
        if (currentFrameType != null) {
            model.addAttribute("equippedFrame", shopItemRepository.findByAssetType(currentFrameType));
        } else {
            model.addAttribute("equippedFrame", null);
        }

        String currentAvatarType = freshUser.getCurrentAvatar();
        if (currentAvatarType != null) {
            model.addAttribute("equippedAvatar", shopItemRepository.findByAssetType(currentAvatarType));
        } else {
            model.addAttribute("equippedAvatar", null);
        }

        // 5. 回傳更衣室局部片段
        return "dashboard/wardrobe :: wardrobe-section";
    }
}