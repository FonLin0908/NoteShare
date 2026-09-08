package com.example.demo.model.studyroom;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "user_daily_shop")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class UserDailyShop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "furniture_id", nullable = false)
    private FurnitureSpec furniture;

    @Column(nullable = false)
    private Integer slotIndex; // 櫥窗位置 (0 ~ 5)

    @Column(nullable = false)
    private Integer discountPrice; // 特價金額

    @Column(nullable = false)
    private Boolean isBought; // 當天是否已購買

    @Column(nullable = false)
    private LocalDate refreshDate; // 刷新日期
}