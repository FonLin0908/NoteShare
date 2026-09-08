package com.example.demo.repository.studyroom;

import com.example.demo.model.studyroom.UserInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserInventoryRepository extends JpaRepository<UserInventory, Long> {

    // 查詢指定玩家的所有背包家具
    List<UserInventory> findByUserId(Long userId);

    // 查詢玩家是否擁有某特定家具
    Optional<UserInventory> findByUserIdAndFurnitureId(Long userId, String furnitureId);
}