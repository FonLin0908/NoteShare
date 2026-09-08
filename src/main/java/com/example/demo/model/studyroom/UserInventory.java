package com.example.demo.model.studyroom;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_inventory", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "furniture_id"})
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class UserInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "furniture_id", nullable = false)
    private FurnitureSpec furniture;

    @Column(nullable = false)
    private Integer totalOwned; // 玩家持有的總數量
}