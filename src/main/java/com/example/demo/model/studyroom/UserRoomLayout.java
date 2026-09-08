package com.example.demo.model.studyroom;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_room_layout")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class UserRoomLayout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "furniture_id", nullable = false)
    private FurnitureSpec furniture;

    @Column(nullable = false)
    private Integer gridX;

    @Column(nullable = false)
    private Integer gridY;
}