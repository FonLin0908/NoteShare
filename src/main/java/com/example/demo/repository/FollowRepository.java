package com.example.demo.repository;

import com.example.demo.model.Follow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface FollowRepository extends JpaRepository<Follow, Long> {

    // 🔍 檢查 A 是否有追蹤 B (用來決定一進網頁顯示「追蹤」還是「取消追蹤」)
    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);

    // 🔍 找出特定的追蹤紀錄 (用來執行取消追蹤時的刪除)
    Optional<Follow> findByFollowerIdAndFollowingId(Long followerId, Long followingId);

    // 📊 統計該用戶追蹤了多少人
    int countByFollowerId(Long followerId);

    // 📊 統計該用戶擁有多少粉絲（被多少人追蹤）
    int countByFollowingId(Long followingId);
}