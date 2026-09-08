package com.example.demo.repository;

import com.example.demo.model.UserChronicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserChronicleRepository extends JpaRepository<UserChronicle, Long> {
    // 依據使用者 ID 撈取所有歷程，並按照時間由新到舊（降序）排列
    List<UserChronicle> findByUserIdOrderByCreatedAtDesc(Long userId);

    // 🎯 檢查該玩家是否已經存在某個特定類型的歷史紀錄（防止重複加載）
    boolean existsByUserIdAndType(Long userId, String type);
}