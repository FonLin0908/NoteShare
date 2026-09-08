package com.example.demo.service;

import com.example.demo.model.ShopItem;
import com.example.demo.model.User;
import com.example.demo.model.UserUnlockedAsset;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.NoteRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.UserUnlockedAssetRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    private final NoteRepository noteRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final UserUnlockedAssetRepository userUnlockedAssetRepository;

    /**
     * 計算使用者收到的筆記與留言按讚總數。
     */
    public int calculateTotalInteractions(Long userId) {
        int noteLikes = noteRepository.sumLikesCountByUserId(userId);

        int commentLikes = commentRepository.sumLikesCountByUserId(userId);

        return noteLikes + commentLikes;
    }
    public boolean updatePassword(User user, String oldRawPassword, String newRawPassword) {
        // 無既有密碼時無法驗證密碼變更請求。
        if (user.getPassword() == null) {
            return false;
        }

        // 先驗證舊密碼，再允許更新。
        if (!passwordEncoder.matches(oldRawPassword, user.getPassword())) {
            return false;
        }

        // 新密碼必須經 BCrypt 雜湊後才能儲存。
        String encodedNewPassword = passwordEncoder.encode(newRawPassword);
        user.setPassword(encodedNewPassword);

        // 存入 PostgreSQL 實體
        userRepository.save(user);
        return true;
    }
    public User save(User user) {
        return userRepository.save(user);
    }

    public boolean hasUnlocked(Long userId, String asset) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return false;
        return user.getUnlockedAssets().contains(asset);
    }

    /**
     * 儲存扣款與發放商品後的使用者狀態。
     */
    @Transactional
    public void deductAndUnlockAsset(Long userId, ShopItem item) {
        // 1. 🔍 安全鎖定：從資料庫撈出最新的使用者本體，撈不到直接觸發事務回滾
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("🛡️ 黑市防禦提示: 找不到該名冒險者！"));

        // 2. 🪙 資料庫層面雙重扣除金幣 (吃的是資料庫 shop_items 表撈出來的 price，非寫死數值)
        if (user.getCoins() < item.getPrice()) {
            throw new RuntimeException("🛡️ 黑市防禦提示: 金幣不足，交易非法重置！");
        }
        user.setCoins(user.getCoins() - item.getPrice());

        // 3. 💾 持久化更新使用者餘額
        userRepository.save(user);

        // 4. 📦 將外觀資產發貨到獨立的 user_unlocked_assets 倉庫表
        UserUnlockedAsset newAsset = new UserUnlockedAsset();
        newAsset.setUserId(user.getId());
        newAsset.setAssetType(item.getAssetType()); // 寫入商品唯一暗號 (如 "AVATAR_FRAME_NEON")

        // 5. 物理落庫儲存
        userUnlockedAssetRepository.save(newAsset);
    }

    /**
     * 取得 Thymeleaf 判斷解鎖狀態所需的資產類型清單。
     */
    public List<String> getUnlockedAssetStrings(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("找不到該冒險者"));

        if (user.getUnlockedAssets() == null) {
            return new ArrayList<>();
        }

        return user.getUnlockedAssets().stream()
                .map(UserUnlockedAsset::getAssetType)
                .collect(Collectors.toList());
    }

    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

}
