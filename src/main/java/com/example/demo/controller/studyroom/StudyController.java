package com.example.demo.controller.studyroom;

import com.example.demo.model.User;
import com.example.demo.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/study")
public class StudyController {

    @Autowired
    private UserService userService;

    @GetMapping
    public String studyHubPage(Model model, HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/login"; // 或者回傳未登入的錯誤片段
        }

        User user = userService.findById(loginUser.getId());
        model.addAttribute("user", user);

        return "study/hub"; // 映射到 templates/study/hub.html
    }
}