package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.service.GamerProfileService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/quests")
public class QuestApiController {

    @Autowired private GamerProfileService gamerProfileService;

    /**
     * 前端異步領取獎勵接口
     * POST /api/quests/claim
     */
    @PostMapping("/claim")
    public ResponseEntity<?> claimReward(@RequestBody Map<String, Long> requestBody, HttpSession session) {
        try {
            Long progressId = requestBody.get("progressId");
            User user = (User) session.getAttribute("loginUser");
            String username = user.getUsername();

            // 呼叫任務結算
            gamerProfileService.claimQuestReward(progressId, username);

            // 領取成功，回傳成功訊息
            return ResponseEntity.ok(Map.of("success", true, "message", "🎉 恭喜完成任務！獎勵已入帳！"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}