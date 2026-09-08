package com.example.demo.repository;

import com.example.demo.model.UserUnlockedAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserUnlockedAssetRepository extends JpaRepository<UserUnlockedAsset, Long> {

    // 🔍 精準查重：看該玩家有沒有這件商品的擁有權
    Optional<UserUnlockedAsset> findByUserIdAndAssetType(Long userId, String assetType);

    // 🛰️ 批次查詢：撈出該玩家解鎖過的所有資產紀錄
    List<UserUnlockedAsset> findByUserId(Long userId);
}