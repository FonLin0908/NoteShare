package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.dto.AchievementProgressDTO;
import com.example.demo.service.AchievementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.List;

@RestController
@RequestMapping("/api/achievements")
public class AchievementController {

    @Autowired
    private AchievementService achievementService;

    /**
     * 取得當前登入使用者的成就牆清單
     */
    @GetMapping("/achievements")
    public String getAchievementsSection(HttpSession session, Model model) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/login"; // 或者回傳未登入的錯誤片段
        }

        // 撈取資料並塞入 Thymeleaf 的 Model 裡面
        List<AchievementProgressDTO> list = achievementService.getUserAchievementList(loginUser.getId());
        model.addAttribute("achievements", list);

        // 回傳剛剛寫好的 achievements.html 底下的 achievements-section 片段
        return "fragments/achievements :: achievements-section";
    }

}