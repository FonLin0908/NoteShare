package com.example.demo.controller;

import com.example.demo.model.Feature;
import com.example.demo.model.ShopItem;
import com.example.demo.model.User;
import com.example.demo.repository.FeatureRepository;
import com.example.demo.repository.ShopItemRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
public class IndexController {

    @Autowired
    private FeatureRepository featureRepository;
    @Autowired
    private ShopItemRepository shopItemRepository;

    @GetMapping("/")
    public String index(Model model, HttpSession session) {
        // 1. 如果資料庫是空的，初始化三筆測試資料到 Supabase
        if (featureRepository.count() == 0) {
            Feature f1 = new Feature();
            f1.setTitle("Supabase 雲端資料庫");
            f1.setDescription("成功連線至 Supabase PostgreSQL，資料正即時在雲端儲存與讀取。");
            f1.setIconType("database");

            Feature f2 = new Feature();
            f2.setTitle("Spring Data JPA");
            f2.setDescription("後端完全不需要寫複雜的 SQL，透過 Repository 介面輕鬆搞定 CRUD。");
            f2.setIconType("cpu");

            featureRepository.saveAll(List.of(f1, f2));
        }
        if(true){
            return "redirect:/notes";
        }

        // 【核心新增】從 Session 撈取當前登入的 user 物件
        User currentUser = (User) session.getAttribute("loginUser");
        User user = currentUser;
        model.addAttribute("user", currentUser); // 傳給首頁 index.html

        // 2. 從 Supabase 撈出卡片資料
        List<Feature> featureList = featureRepository.findAll();
        model.addAttribute("features", featureList);

        // 3. 時間資料
        String formattedTime = com.example.demo.config.AppClock.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        model.addAttribute("serverTime", formattedTime);

        if(user != null) {
            String currentFrameType = user.getCurrentFrame(); // 比如拿到 "FRAME_LAVA"

            //全部外觀屬性(頭相框)
            if (currentFrameType != null) {
                // 去資料庫把這件商品的完整屬性（包含 style_value）撈出來
                ShopItem equippedFrame = shopItemRepository.findByAssetType(currentFrameType);
                model.addAttribute("equippedFrame", equippedFrame);
            } else {
                model.addAttribute("equippedFrame", null);
            }

            String currentAvatarType = user.getCurrentAvatar(); // 比如拿到 "AVATAR_CAT"

            if (currentAvatarType != null) {
                // 透過我們剛才在 Repository 焊好的黃金方法，直接撈出頭像道具的完整資料
                ShopItem equippedAvatar = shopItemRepository.findByAssetType(currentAvatarType);
                model.addAttribute("equippedAvatar", equippedAvatar);
            } else {
                model.addAttribute("equippedAvatar", null);
            }

        }

        return "index";
    }
}