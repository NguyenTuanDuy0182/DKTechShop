package com.ecommerce.dktechshop.data.model;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Product implements Serializable {
    private String id = "";
    private String name = "";
    private String brand = "";
    private String category = "";
    private String categoryLabel = "";
    private long price = 0L;
    private long originalPrice = 0L;
    private int discountPercent = 0;
    private double rating = 5.0;
    private int reviewCount = 0;
    private int stock = 0;
    private String thumbnailUrl = "";
    private String description = "";
    private Map<String, String> specs = new HashMap<>();
    private List<Object> variants = new ArrayList<>();

    // Mandatory no-arg constructor for Firestore deserialization
    public Product() {}

    public Product(String id, String name, String brand, String category, String categoryLabel,
                   long price, long originalPrice, int discountPercent, double rating,
                   int reviewCount, int stock, Map<String, String> specs, List<?> variants) {
        this.id = id;
        this.name = name;
        this.brand = brand;
        this.category = category;
        this.categoryLabel = categoryLabel;
        this.price = price;
        this.originalPrice = originalPrice;
        this.discountPercent = discountPercent;
        this.rating = rating;
        this.reviewCount = reviewCount;
        this.stock = stock;
        this.specs = specs != null ? specs : new HashMap<>();
        this.variants = variants != null ? new ArrayList<>(variants) : new ArrayList<>();
        this.thumbnailUrl = "";
    }

    public Product(String id, String name, String brand, String category, String categoryLabel,
                   long price, long originalPrice, int discountPercent, double rating,
                   int reviewCount, int stock, Map<String, String> specs, List<?> variants,
                   String thumbnailUrl) {
        this(id, name, brand, category, categoryLabel, price, originalPrice, discountPercent, rating, reviewCount, stock, specs, variants);
        this.thumbnailUrl = thumbnailUrl != null ? thumbnailUrl : "";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getCategoryLabel() { return categoryLabel; }
    public void setCategoryLabel(String categoryLabel) { this.categoryLabel = categoryLabel; }
    public long getPrice() { return price; }
    public void setPrice(long price) { this.price = price; }
    public long getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(long originalPrice) { this.originalPrice = originalPrice; }
    public int getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(int discountPercent) { this.discountPercent = discountPercent; }
    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }
    public int getReviewCount() { return reviewCount; }
    public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Map<String, String> getSpecs() { return specs; }
    public void setSpecs(Map<String, String> specs) { this.specs = specs; }
    @Exclude
    public List<String> getVariants() {
        List<String> result = new ArrayList<>();
        if (variants != null) {
            for (Object obj : variants) {
                if (obj instanceof String) {
                    String s = (String) obj;
                    if (!s.trim().isEmpty()) {
                        result.add(s);
                    }
                } else if (obj instanceof Map) {
                    Map<?, ?> map = (Map<?, ?>) obj;
                    if (map.containsKey("name") && map.get("name") != null) {
                        result.add(String.valueOf(map.get("name")));
                    } else if (map.containsKey("variant") && map.get("variant") != null) {
                        result.add(String.valueOf(map.get("variant")));
                    } else if (map.containsKey("title") && map.get("title") != null) {
                        result.add(String.valueOf(map.get("title")));
                    } else if (map.containsKey("label") && map.get("label") != null) {
                        result.add(String.valueOf(map.get("label")));
                    } else if (map.containsKey("color") && map.get("color") != null) {
                        result.add(String.valueOf(map.get("color")));
                    } else if (map.containsKey("value") && map.get("value") != null) {
                        result.add(String.valueOf(map.get("value")));
                    } else if (!map.isEmpty()) {
                        result.add(String.valueOf(map.values().iterator().next()));
                    }
                } else if (obj != null) {
                    result.add(obj.toString());
                }
            }
        }
        if (result.isEmpty()) {
            result.add("Mặc định");
        }
        return result;
    }

    @PropertyName("variants")
    public List<Object> getRawVariants() {
        return variants;
    }

    @PropertyName("variants")
    public void setVariants(List<Object> variants) {
        this.variants = variants != null ? variants : new ArrayList<>();
    }
}
