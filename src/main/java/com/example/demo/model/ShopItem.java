package com.example.demo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "shop_items")
public class ShopItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "asset_type", unique = true, nullable = false)
    private String assetType;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssetCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssetSeries series;

    @Column(nullable = false)
    private Integer price;

    @Column(name = "required_level")
    private Integer requiredLevel = 1;

    @Column(name = "is_visible_in_regular")
    private Boolean isVisibleInRegular = true;

    @Column(name = "is_available_in_limited")
    private Boolean isAvailableInLimited = true;

    @Column(name = "style_value")
    private String styleValue;

    // 🟢 保持程式碼乾淨，以下補上標準的 Getters 和 Setters...
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAssetType() { return assetType; }
    public void setAssetType(String assetType) { this.assetType = assetType; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public AssetCategory getCategory() { return category; }
    public void setCategory(AssetCategory category) { this.category = category; }
    public AssetSeries getSeries() { return series; }
    public void setSeries(AssetSeries series) { this.series = series; }
    public Integer getPrice() { return price; }
    public void setPrice(Integer price) { this.price = price; }
    public Integer getRequiredLevel() { return requiredLevel; }
    public void setRequiredLevel(Integer requiredLevel) { this.requiredLevel = requiredLevel; }
    public Boolean getIsVisibleInRegular() { return isVisibleInRegular; }
    public void setIsVisibleInRegular(Boolean isVisibleInRegular) { this.isVisibleInRegular = isVisibleInRegular; }
    public Boolean getIsAvailableInLimited() { return isAvailableInLimited; }
    public void setIsAvailableInLimited(Boolean isAvailableInLimited) { this.isAvailableInLimited = isAvailableInLimited; }
    public String getStyleValue() { return styleValue; }
    public void setStyleValue(String styleValue) { this.styleValue = styleValue; }
}