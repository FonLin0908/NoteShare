package com.example.demo.controller;

import com.example.demo.model.ReportRequest;
import com.example.demo.model.Report;
import com.example.demo.model.ReportReason;
import com.example.demo.model.User;
import com.example.demo.repository.ReportRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/notes/api")
public class ReportApiController {

    @Autowired
    private ReportRepository reportRepository;

    @PostMapping("/report")
    public ResponseEntity<Map<String, Object>> submitReport(
            @RequestBody ReportRequest request,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();

        // 檢舉操作僅限已登入使用者。
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            response.put("success", false);
            response.put("message", "請先登入系統後再進行檢舉！");
            return ResponseEntity.status(401).body(response);
        }

        try {
            // 2. 轉換並封裝資料
            Report report = new Report();
            report.setReporter(loginUser);
            report.setTargetId(request.getTargetId());
            report.setTargetType(request.getTargetType());

            // 將前端傳來的字串（如 "PLAGIARISM"）轉成後端的 Enum
            report.setReasonCategory(ReportReason.valueOf(request.getReasonCategory()));
            report.setDescription(request.getDescription());
            report.setStatus("PENDING"); // 初始狀態為待處理

            // 3. 寫入資料庫
            reportRepository.save(report);

            // 4. 回傳成功訊號
            response.put("success", true);
            response.put("message", "檢舉提交成功！");
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", "不合法的檢舉類別！");
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "伺服器內部錯誤，請稍後再試！");
            return ResponseEntity.status(500).body(response);
        }
    }
}
