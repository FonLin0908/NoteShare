package com.example.demo.repository;

import com.example.demo.model.ShopItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ShopItemRepository extends JpaRepository<ShopItem, Long> {

    // 🟢 智能篩選：只查出常駐總覽商店要顯示的商品，並依照金額由低到高排序
    List<ShopItem> findByIsVisibleInRegularTrueOrderByPriceAsc();

    // 🔍 單一外觀資產查詢（讀取 style_value 或裝備資訊使用）
    ShopItem findByAssetType(String assetType);

    // 👕 核心擴充：根據玩家已解鎖的所有 assetType 清單批次撈出對應的 ShopItem（支援成就限定非賣品）
    List<ShopItem> findByAssetTypeIn(Collection<String> assetTypes);
}