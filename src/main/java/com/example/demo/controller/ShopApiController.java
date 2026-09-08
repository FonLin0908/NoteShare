package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.model.ShopItem;
import com.example.demo.repository.ShopItemRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.ShopService;
import com.example.demo.service.UserChronicleService;
import com.example.demo.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/shop")
public class ShopApiController {

    @Autowired
    private ShopService shopService;

    @Autowired
    private ShopItemRepository shopItemRepository; // 用來物理核對商品價格
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private UserChronicleService chronicleService;

    @PostMapping("/buy")
    public ResponseEntity<?> buyAsset(@RequestBody Map<String, String> payload, HttpSession session) {

        // 1. 🛡️ 鋼鐵防線一：Session 安全驗證
        User sessionUser = (User) session.getAttribute("loginUser");
        if (sessionUser == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "🔒 冒險者，傳送門失效，請重新登入！"));
        }

        // 2. 🛡️ 鋼鐵防線二：驗證前端傳過來的商品代碼是否存在於資料庫
        String assetType = payload.get("assetType");
        ShopItem item = shopItemRepository.findByAssetType(assetType);

        if (item == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "❌ 該商品無法在此通路販售或已下架！"));
        }

        // 3. 🛡️ 鋼鐵防線三：等級門檻盲算攔截（解鎖等級判定）
        if (sessionUser.getLevel() < item.getRequiredLevel()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message",
                    "🔒 等級不足！該物資需要達 Lv." + item.getRequiredLevel() + " 才有資格兌換！"));
        }

        // 4. 🛡️ 鋼鐵防線四：去解鎖倉庫表查重，防止重複購買
        if (shopService.hasUnlocked(sessionUser.getId(), assetType)) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "🛡️ 您早已擁有此項外觀權限，無須重複購買！"));
        }

        // 5. 🛡️ 鋼鐵防線五：安全核對餘額
        if (sessionUser.getCoins() < item.getPrice()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "🪙 金幣不足！去多發點優質筆記賺取賞金吧！"));
        }

        shopService.deductAndUnlockAsset(sessionUser, item);
        User dbUser = userRepository.findById(sessionUser.getId()).orElse(sessionUser); // 🛡️ 撈出金幣扣完後的真身
        session.setAttribute("loginUser", dbUser); // ✅ 把完全乾淨、扣完錢的真身灌回 Session

        String title = "商城購買【" + item.getDisplayName() + "】";

        String content = String.format("成功在商城消耗 🪙 %d Coin，購買解鎖個人化外觀：《%s》！",
                item.getPrice(),
                item.getDisplayName()
        );

        // 寫入編年史
        chronicleService.recordEvent(
                dbUser.getId(),
                "SHOP_BUY",                 // type: 標記為商城購買事件
                title,                      // title: 黑市解鎖【幻彩星空框】
                content,                    // content: 成功在補給黑市消耗...
                "bi-bag-check-fill",        // iconClass: 購物袋/解鎖圖示
                "bg-warning"                // badgeColor: 耀眼金黃色
        );

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "🎉 成功兌換：" + item.getDisplayName() + "！權限已寫入倉庫。"
        ));
    }
    // 📄 在 ShopApiController.java 內部追加：

    // 📄 修改 ShopApiController.java 內部的 /equip 路由

    // 📄 修改 ShopApiController.java 內部的 /api/shop/equip 端點：

    @PostMapping("/equip")
    public ResponseEntity<?> equipAsset(@RequestBody EquipRequest request, HttpServletRequest servletRequest) {
        // 🟢 1. 取得當前會話的 HttpSession
        HttpSession session = servletRequest.getSession();
        User user = (User) session.getAttribute("loginUser");

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "會話已過期，請重新登入"));
        }

        String assetType = request.getAssetType();
        String category = request.getCategory();
        String action = request.getAction();
        String styleValue = "";

        // 🟢 2. 依據動作執行穿脫欄位修改
        if ("EQUIP".equalsIgnoreCase(action)) {
            if ("FRAME".equals(category)) { user.setCurrentFrame(assetType); }
            else if ("BG_COLOR".equals(category)) { user.setCurrentBgColor(assetType); }
            else if ("TITLE".equals(category)) { user.setCurrentTitle(assetType); }
            else if ("IMAGE".equals(category)) { user.setCurrentAvatar(assetType); }

            // 🚀 ===================================================================
            // 🟢 【精準修正】：直接呼叫我們建好的 Repository 撈出商品完全體，並安全取出 style_value！
            // ===================================================================
            ShopItem item = shopItemRepository.findByAssetType(assetType);
            styleValue = (item != null) ? item.getStyleValue() : "";
            // ===================================================================

        } else { // UNEQUIP / REMOVE
            if ("FRAME".equals(category)) { user.setCurrentFrame(null); }
            else if ("BG_COLOR".equals(category)) { user.setCurrentBgColor(null); }
            else if ("TITLE".equals(category)) { user.setCurrentTitle(null); }
            else if ("IMAGE".equals(category)) { user.setCurrentAvatar(null); }
        }

        // 🟢 3. 💾 寫入資料庫永久保存
        User updatedUser = userService.save(user);

        // 🚀 ===================================================================
        // 🟢 【HttpSession 黃金防線】：把資料庫最新吐出來的物件重灌回 Session 記憶體！
        // ===================================================================
        session.setAttribute("loginUser", updatedUser);
        // ===================================================================

        // 🟢 4. 回送給前端 SPA 引擎
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "EQUIP".equalsIgnoreCase(action) ? "⚔️ 外觀配戴成功！" : "❌ 外觀卸除成功！");
        response.put("styleValue", styleValue);

        return ResponseEntity.ok(response);
    }

    public static class EquipRequest {
        private String assetType;
        private String category;
        private String action;

        // 🟢 必備的 Getters 和 Setters
        public String getAssetType() { return assetType; }
        public void setAssetType(String assetType) { this.assetType = assetType; }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
    }
}
