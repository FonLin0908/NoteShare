package com.example.demo.repository;

import com.example.demo.model.Feature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FeatureRepository extends JpaRepository<Feature, Long> {
    // 繼承後就自動擁有新增、刪除、修改、查詢功能！
}