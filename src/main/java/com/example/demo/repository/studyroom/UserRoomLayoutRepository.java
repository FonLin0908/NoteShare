package com.example.demo.repository.studyroom;

import com.example.demo.model.studyroom.UserRoomLayout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRoomLayoutRepository extends JpaRepository<UserRoomLayout, Long> {

    // 查詢指定玩家目前小屋的所有擺設家具與座標
    List<UserRoomLayout> findByUserId(Long userId);

    // 清空玩家目前的擺設（用於重寫/更新整間小屋的佈局）
    void deleteByUserId(Long userId);
}