package com.example.demo.initializer;

import com.example.demo.model.studyroom.FurnitureSpec;
import com.example.demo.repository.studyroom.FurnitureSpecRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class StudyRoomInitializer implements InitializerTask {

    private final FurnitureSpecRepository furnitureSpecRepository;

    @Override
    public String getTaskName() {
        return "專注小屋預設家具初始化引擎";
    }

    @Override
    public void execute() {
        // 1. 檢查資料庫是否已有家具範本，若有則跳過初始化
        if (furnitureSpecRepository.count() > 0) {
            System.out.println("  └─ 📦 [專注小屋] 資料庫已有家具規格資料，跳過預設載入。");
            return;
        }

        String defaultSprite = "/images/furniture/Interiors_free_16x16.png";

        // 2. 建立初始家具圖鑑
        List<FurnitureSpec> defaultFurnitures = List.of(
                FurnitureSpec.builder()
                        .id("f_desk_01")
                        .name("雙人桌椅")
                        .spriteUrl(defaultSprite)
                        .category(FurnitureSpec.Category.FURNITURE)
                        .pxX(121).pxY(618).pxW(27).pxH(21)
                        .tileW(2.0f).tileH(1.5f)
                        .isWalkable(false)
                        .price(150)
                        .isCatalog(true)
                        .build(),

                FurnitureSpec.builder()
                        .id("f_rug_01")
                        .name("風格地毯")
                        .spriteUrl(defaultSprite)
                        .category(FurnitureSpec.Category.DECORATION)
                        .pxX(288).pxY(64).pxW(48).pxH(32)
                        .tileW(3.0f).tileH(2.0f)
                        .isWalkable(true)
                        .price(80)
                        .isCatalog(true)
                        .build(),

                FurnitureSpec.builder()
                        .id("f_chair_01")
                        .name("單人木椅")
                        .spriteUrl(defaultSprite)
                        .category(FurnitureSpec.Category.FURNITURE)
                        .pxX(80).pxY(618).pxW(16).pxH(20)
                        .tileW(1.0f).tileH(1.0f)
                        .isWalkable(false)
                        .price(50)
                        .isCatalog(true)
                        .build(),

                FurnitureSpec.builder()
                        .id("f_plant_01")
                        .name("室內盆栽")
                        .spriteUrl(defaultSprite)
                        .category(FurnitureSpec.Category.DECORATION)
                        .pxX(16).pxY(320).pxW(16).pxH(32)
                        .tileW(1.0f).tileH(1.0f)
                        .isWalkable(false)
                        .price(60)
                        .isCatalog(true)
                        .build(),

                FurnitureSpec.builder()
                        .id("f_lamp_01")
                        .name("讀書檯燈")
                        .spriteUrl(defaultSprite)
                        .category(FurnitureSpec.Category.FUNCTIONAL)
                        .pxX(144).pxY(320).pxW(16).pxH(16)
                        .tileW(1.0f).tileH(1.0f)
                        .isWalkable(false)
                        .price(120)
                        .isCatalog(true)
                        .build()
        );

        furnitureSpecRepository.saveAll(defaultFurnitures);
        System.out.println("  └─ 🛋️ [專注小屋] 成功寫入 " + defaultFurnitures.size() + " 種預設家具資料！");
    }
}