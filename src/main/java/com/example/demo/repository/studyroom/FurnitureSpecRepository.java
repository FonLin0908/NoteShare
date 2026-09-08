package com.example.demo.repository.studyroom;

import com.example.demo.model.studyroom.FurnitureSpec;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FurnitureSpecRepository extends JpaRepository<FurnitureSpec, String> {

    // 查詢常駐目錄中所有上架的家具
    List<FurnitureSpec> findByIsCatalogTrue();

    // 依據分類查詢常駐目錄中的家具
    List<FurnitureSpec> findByIsCatalogTrueAndCategory(FurnitureSpec.Category category);
}