package com.example.demo.model.studyroom;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "furniture_spec")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class FurnitureSpec {

    @Id
    @Column(length = 64)
    private String id; // 例如: f_desk_01

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 255)
    private String spriteUrl; // 素材圖片路徑/連結

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Category category; // FURNITURE, DECORATION, FUNCTIONAL, TROPHY

    private Integer pxX;
    private Integer pxY;
    private Integer pxW;
    private Integer pxH;

    private Float tileW;
    private Float tileH;

    private Boolean isWalkable;
    private Integer price;
    private Boolean isCatalog;

    public enum Category {
        FURNITURE,   // 基礎家具
        DECORATION,  // 裝飾物件
        FUNCTIONAL,  // 功能建築
        TROPHY       // 成就紀念
    }
}