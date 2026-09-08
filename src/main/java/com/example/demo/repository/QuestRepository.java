package com.example.demo.repository;

import com.example.demo.model.Quest;
import com.example.demo.model.QuestType;
import com.example.demo.model.UserAction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QuestRepository extends JpaRepository<Quest, Long> {
    // 🔍 核心查詢：根據週期類型（如 DAILY）以及觸發動作（如 COMMENT）撈出對應的任務
    List<Quest> findByActionType(UserAction targetAction);
    List<Quest> findByType(QuestType questType);
}