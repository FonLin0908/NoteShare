package com.example.demo.repository.studyroom;

import com.example.demo.model.studyroom.UserDailyShop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserDailyShopRepository extends JpaRepository<UserDailyShop, Long> {

    // 查詢玩家當天生成的 6 個每日特賣櫃位
    List<UserDailyShop> findByUserIdAndRefreshDate(Long userId, LocalDate refreshDate);

    // 查詢玩家當天特定位置的櫃位
    Optional<UserDailyShop> findByUserIdAndRefreshDateAndSlotIndex(Long userId, LocalDate refreshDate, Integer slotIndex);

    // 清理舊日期的每日特賣紀錄（可定期維護資料庫）
    void deleteByUserIdAndRefreshDateBefore(Long userId, LocalDate refreshDate);
}