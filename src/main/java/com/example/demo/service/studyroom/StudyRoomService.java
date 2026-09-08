package com.example.demo.service.studyroom;

import com.example.demo.model.studyroom.*;
import com.example.demo.repository.studyroom.*;
import com.example.demo.service.ShopService;
import com.example.demo.service.studyroom.dto.RoomLayoutRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StudyRoomService {

    private final FurnitureSpecRepository furnitureSpecRepository;
    private final UserInventoryRepository userInventoryRepository;
    private final UserRoomLayoutRepository userRoomLayoutRepository;
    private final UserDailyShopRepository userDailyShopRepository;

    private final ShopService shopService;
    // 🟢 1. 取得指定玩家的背包資料
    public List<UserInventory> getUserInventory(Long userId) {
        return userInventoryRepository.findByUserId(userId);
    }

    // 🟢 2. 取得指定玩家當前的小屋佈局
    public List<UserRoomLayout> getUserRoomLayout(Long userId) {
        return userRoomLayoutRepository.findByUserId(userId);
    }

    // 🟢 3. 儲存/更新玩家的小屋擺設 (覆蓋式儲存)
    @Transactional
    public List<UserRoomLayout> saveRoomLayout(Long userId, List<RoomLayoutRequest> layoutRequests) {
        // 先清空該玩家原本的佈局紀錄
        userRoomLayoutRepository.deleteByUserId(userId);

        if (layoutRequests == null || layoutRequests.isEmpty()) {
            return Collections.emptyList();
        }

        List<UserRoomLayout> newLayouts = new ArrayList<>();
        for (RoomLayoutRequest req : layoutRequests) {
            FurnitureSpec spec = furnitureSpecRepository.findById(req.getFurnitureId())
                    .orElseThrow(() -> new IllegalArgumentException("找不到家具規格: " + req.getFurnitureId()));

            UserRoomLayout layout = UserRoomLayout.builder()
                    .userId(userId)
                    .furniture(spec)
                    .gridX(req.getGridX())
                    .gridY(req.getGridY())
                    .build();
            newLayouts.add(layout);
        }

        return userRoomLayoutRepository.saveAll(newLayouts);
    }

    // 🟢 4. 從常駐目錄購買家具
    @Transactional
    public UserInventory buyFromCatalog(Long userId, String furnitureId) {
        FurnitureSpec spec = furnitureSpecRepository.findById(furnitureId)
                .orElseThrow(() -> new IllegalArgumentException("找不到指定家具: " + furnitureId));

        // TODO: 在同一交易內驗證餘額並扣除家具價格；完成前不應開放此購買流程。

        if (!shopService.checkAndBuy(userId, spec.getPrice())){
            throw new IllegalStateException("餘額不足");
        }

        UserInventory inventory = userInventoryRepository.findByUserIdAndFurnitureId(userId, furnitureId)
                .orElse(UserInventory.builder()
                        .userId(userId)
                        .furniture(spec)
                        .totalOwned(0)
                        .build());

        inventory.setTotalOwned(inventory.getTotalOwned() + 1);
        return userInventoryRepository.save(inventory);
    }

    // 🟢 5. 取得或產生玩家當天 6 格每日特賣
    @Transactional
    public List<UserDailyShop> getOrGenerateDailyShop(Long userId) {
        LocalDate today = LocalDate.now();
        List<UserDailyShop> existingShop = userDailyShopRepository.findByUserIdAndRefreshDate(userId, today);

        if (!existingShop.isEmpty()) {
            return existingShop;
        }

        // 當天若無紀錄則隨機抽取 6 個常駐家具
        List<FurnitureSpec> catalogSpecs = furnitureSpecRepository.findByIsCatalogTrue();
        if (catalogSpecs.isEmpty()) {
            return Collections.emptyList();
        }

        Collections.shuffle(catalogSpecs);
        int count = Math.min(6, catalogSpecs.size());
        List<UserDailyShop> newDailyShop = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            FurnitureSpec spec = catalogSpecs.get(i);
            int discountPrice = Math.max(1, (int) (spec.getPrice() * 0.7)); // 預設 7 折特價

            UserDailyShop slot = UserDailyShop.builder()
                    .userId(userId)
                    .furniture(spec)
                    .slotIndex(i)
                    .discountPrice(discountPrice)
                    .isBought(false)
                    .refreshDate(today)
                    .build();
            newDailyShop.add(slot);
        }

        return userDailyShopRepository.saveAll(newDailyShop);
    }

    // 🟢 6. 購買每日特賣物品 (限購一次)
    @Transactional
    public UserDailyShop buyDailyShopItem(Long userId, Integer slotIndex) {
        LocalDate today = LocalDate.now();
        UserDailyShop shopSlot = userDailyShopRepository.findByUserIdAndRefreshDateAndSlotIndex(userId, today, slotIndex)
                .orElseThrow(() -> new IllegalArgumentException("找不到當日特賣商品區塊"));

        if (shopSlot.getIsBought()) {
            throw new IllegalStateException("該商品本日已購買售罄");
        }

        if (!shopService.checkAndBuy(userId, shopSlot.getSlotIndex())){
            throw new IllegalStateException("餘額不足");
        }
        // TODO: 在同一交易內驗證餘額並扣除每日商店折扣價；完成前不應開放此購買流程。

        // 標記為已售罄
        shopSlot.setIsBought(true);
        userDailyShopRepository.save(shopSlot);

        // 將家具加入玩家背包
        UserInventory inventory = userInventoryRepository.findByUserIdAndFurnitureId(userId, shopSlot.getFurniture().getId())
                .orElse(UserInventory.builder()
                        .userId(userId)
                        .furniture(shopSlot.getFurniture())
                        .totalOwned(0)
                        .build());

        inventory.setTotalOwned(inventory.getTotalOwned() + 1);
        userInventoryRepository.save(inventory);

        return shopSlot;
    }
}
