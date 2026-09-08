package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.repository.ShopItemRepository;
import com.example.demo.service.RankingService;
import com.example.demo.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class RankingController {

    @Autowired
    private RankingService rankingService;

    @Autowired
    private UserService userService;

    @Autowired
    private ShopItemRepository shopItemRepository;

    @GetMapping("/ranking")
    public String showRankingPage(
            @RequestParam(name = "tab", defaultValue = "weekly") String tab,
            Model model, HttpSession session) {

        User loginUser = (User) session.getAttribute("loginUser");

        if (loginUser != null) {
            // 將 user 塞入 model 供 Thymeleaf 導覽列渲染
            model.addAttribute("user", loginUser);

            String currentFrameType = loginUser.getCurrentFrame();
            if (currentFrameType != null) {
                model.addAttribute("equippedFrame", shopItemRepository.findByAssetType(currentFrameType));
            } else {
                model.addAttribute("equippedFrame", null);
            }

            String currentAvatarType = loginUser.getCurrentAvatar();
            if (currentAvatarType != null) {
                model.addAttribute("equippedAvatar", shopItemRepository.findByAssetType(currentAvatarType));
            } else {
                model.addAttribute("equippedAvatar", null);
            }
        }
        model.addAttribute("activeTab", tab);

        // 根據 tab 參數載入對應榜單資料
        switch (tab.toLowerCase()) {
            case "monthly":
                model.addAttribute("noteList", rankingService.getMonthlyTopNotes());
                break;
            case "all":
                model.addAttribute("noteList", rankingService.getAllTimeTopNotes());
                break;
            case "authors":
                model.addAttribute("authorList", rankingService.getTopAuthors());
                break;
            case "weekly":
            default:
                model.addAttribute("noteList", rankingService.getWeeklyTopNotes());
                break;
        }

        return "ranking"; // 對應 templates/ranking.html
    }
}