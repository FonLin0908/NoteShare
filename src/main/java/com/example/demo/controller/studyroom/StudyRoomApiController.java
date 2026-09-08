package com.example.demo.controller.studyroom;

import com.example.demo.model.User;
import com.example.demo.model.studyroom.*;
import com.example.demo.service.studyroom.StudyRoomService;
import com.example.demo.service.studyroom.dto.RoomLayoutRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/study-room")
@RequiredArgsConstructor
public class StudyRoomApiController {

    private final StudyRoomService studyRoomService;

    // 從 Session 取得目前登入者 ID；未登入時回傳 null。
    private Long getLoggedInUserId(HttpSession session) {
        User loggedInUser = (User) session.getAttribute("loginUser");
        if (loggedInUser == null) {
            return null;
        }
        return loggedInUser.getId();
    }

    // 🟢 1. 取得玩家背包資料
    @GetMapping("/inventory")
    public ResponseEntity<?> getInventory(HttpSession session) {
        Long userId = getLoggedInUserId(session);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "請先登入系統"));
        }
        return ResponseEntity.ok(studyRoomService.getUserInventory(userId));
    }

    // 🟢 2. 取得小屋當前擺設
    @GetMapping("/layout")
    public ResponseEntity<?> getLayout(HttpSession session) {
        Long userId = getLoggedInUserId(session);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "請先登入系統"));
        }
        return ResponseEntity.ok(studyRoomService.getUserRoomLayout(userId));
    }

    // 🟢 3. 儲存小屋佈局 (完成建造時發送)
    @PostMapping("/layout")
    public ResponseEntity<?> saveLayout(HttpSession session, @RequestBody List<RoomLayoutRequest> requests) {
        Long userId = getLoggedInUserId(session);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "請先登入系統"));
        }
        return ResponseEntity.ok(studyRoomService.saveRoomLayout(userId, requests));
    }

    // 🟢 4. 常駐目錄購買家具 (會扣除金幣)
    @PostMapping("/buy/catalog/{furnitureId}")
    public ResponseEntity<?> buyCatalog(HttpSession session, @PathVariable String furnitureId) {
        Long userId = getLoggedInUserId(session);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "請先登入系統"));
        }

        try {
            UserInventory inventory = studyRoomService.buyFromCatalog(userId, furnitureId);
            return ResponseEntity.ok(inventory);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 🟢 5. 取得玩家當天 6 格每日特賣
    @GetMapping("/daily-shop")
    public ResponseEntity<?> getDailyShop(HttpSession session) {
        Long userId = getLoggedInUserId(session);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "請先登入系統"));
        }
        return ResponseEntity.ok(studyRoomService.getOrGenerateDailyShop(userId));
    }

    // 🟢 6. 購買每日特賣物品 (會扣除特價金幣)
    @PostMapping("/buy/daily/{slotIndex}")
    public ResponseEntity<?> buyDailyItem(HttpSession session, @PathVariable Integer slotIndex) {
        Long userId = getLoggedInUserId(session);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "請先登入系統"));
        }

        try {
            UserDailyShop shopSlot = studyRoomService.buyDailyShopItem(userId, slotIndex);
            return ResponseEntity.ok(shopSlot);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
