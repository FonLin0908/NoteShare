package com.example.demo.service.studyroom.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomLayoutRequest {
    private String furnitureId;
    private Integer gridX;
    private Integer gridY;
}