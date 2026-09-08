package com.example.demo.service;

import com.example.demo.model.ShopItem;
import com.example.demo.model.User;
import com.example.demo.model.UserUnlockedAsset;
import com.example.demo.repository.ShopItemRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.UserUnlockedAssetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ShopService {

    @Autowired
    private ShopItemRepository shopItemRepository;

    @Autowired
    private UserUnlockedAssetRepository userUnlockedAssetRepository;

    @Autowired
    private UserRepository userRepository;

    // 📜 注入冒險者編年史服務
    @Autowired
    private UserChronicleService userChronicleService;

    /**
     * 🛒 商城上架流：只撈出要在常駐商店出現的商品總目錄
     */
    public List<ShopItem> getRegularShopItems() {
        return shopItemRepository.findByIsVisibleInRegularTrueOrderByPriceAsc();
    }

    /**
     * 🔍 權限雷達：精準搜尋玩家是否解鎖過該外觀資產
     */
    public boolean hasUnlocked(Long userId, String assetType) {
        return userUnlockedAssetRepository.findByUserIdAndAssetType(userId, assetType).isPresent();
    }

    /**
     * 🛰️ 轉換引擎：查出該玩家解鎖過的所有資產字串清單 (例如: ["AVATAR_FRAME_NEON", "BG_COLOR_PURPLE"])
     */
    public List<String> getUnlockedAssetStrings(Long userId) {
        List<UserUnlockedAsset> assets = userUnlockedAssetRepository.findByUserId(userId);
        return assets.stream()
                .map(UserUnlockedAsset::getAssetType)
                .collect(Collectors.toList());
    }

    /**
     * 💾 物理扣錢與發貨落庫機制（ACID 鋼鐵事務鎖防禦）
     */
    @Transactional
    public void deductAndUnlockAsset(User user, ShopItem item) {
        // 1. 資料庫層面物理扣除使用者金幣
        user.setCoins(user.getCoins() - item.getPrice());
        userRepository.save(user);

        // 2. 將該外觀資產打入玩家的倉庫表落庫（帶入當前解鎖時間）
        UserUnlockedAsset newAsset = new UserUnlockedAsset();
        newAsset.setUserId(user.getId());
        newAsset.setAssetType(item.getAssetType());
        newAsset.setUnlockedAt(com.example.demo.config.AppClock.now());
        userUnlockedAssetRepository.save(newAsset);

        // 📜 3. 寫入冒險者編年史
        userChronicleService.recordAssetUnlocked(user.getId(), item.getDisplayName());
    }

    /**
     * 🛠️ 萬能外觀裝備與卸除引擎
     * @param action "EQUIP" (裝備) 或 "REMOVE" (移除)
     * @param categoryType "FRAME" 或 "IMAGE" 等分類
     */
    @Transactional
    public User handleAssetEquip(Long userId, String assetType, String action, String categoryType) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("找不到該冒險者"));

        // ❌ 動作一：移除裝備邏輯
        if ("REMOVE".equalsIgnoreCase(action)) {
            switch (categoryType.toUpperCase()) {
                case "FRAME" -> user.setCurrentFrame(null);       // 卸除頭像框
                case "BG_COLOR" -> user.setCurrentBgColor(null);   // 卸除背景色
                case "TITLE" -> user.setCurrentTitle(null);       // 卸除稱號
                case "IMAGE" -> user.setCurrentAvatar(null);       // 卸除自訂頭像
                case "BG_IMAGE" -> user.setCurrentBgImage(null);   // 卸除動態背景圖
            }
        }
        // ⚔️ 動作二：穿上裝備邏輯
        else {
            // 安全防禦線：先驗證玩家在 user_unlocked_assets 表裡到底有沒有這件寶物
            boolean hasOwnership = userUnlockedAssetRepository.findByUserIdAndAssetType(userId, assetType).isPresent();
            if (!hasOwnership) {
                throw new RuntimeException("🛡️ 黑市防禦: 您尚未解鎖該外觀，無法裝備！");
            }

            // 各部位穿戴
            switch (categoryType.toUpperCase()) {
                case "FRAME" -> user.setCurrentFrame(assetType);
                case "BG_COLOR" -> user.setCurrentBgColor(assetType);
                case "TITLE" -> user.setCurrentTitle(assetType);
                case "IMAGE" -> user.setCurrentAvatar(assetType);
                case "BG_IMAGE" -> user.setCurrentBgImage(assetType);
            }
        }

        return userRepository.save(user);
    }

    @Transactional
    public boolean checkAndBuy(Long userId, int price) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getCoins() < price) {
            return false;
        } else {
            user.setCoins(user.getCoins() - price);
            userRepository.save(user);
            return true;
        }
    }
}