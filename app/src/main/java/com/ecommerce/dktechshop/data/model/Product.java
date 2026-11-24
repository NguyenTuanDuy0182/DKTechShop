package com.ecommerce.dktechshop.data.model;

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
    private List<ProductVariant> variants = new ArrayList<>();

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
        setVariants(variants);
        this.thumbnailUrl = "";
        this.description = "";
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
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl != null ? thumbnailUrl : ""; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description != null ? description : ""; }
    public Map<String, String> getSpecs() { return specs; }
    public void setSpecs(Map<String, String> specs) { this.specs = specs != null ? specs : new HashMap<>(); }

    public List<ProductVariant> getVariants() {
        return variants;
    }

    public void setVariants(List<?> rawList) {
        this.variants = new ArrayList<>();
        if (rawList == null) return;
        for (Object item : rawList) {
            if (item instanceof ProductVariant) {
                this.variants.add((ProductVariant) item);
            } else if (item instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) item;
                String name = map.get("name") != null ? map.get("name").toString() : "";
                String colorHex = map.get("colorHex") != null ? map.get("colorHex").toString() : "";
                long extra = 0L;
                Object extraObj = map.get("extraPrice");
                if (extraObj instanceof Number) {
                    extra = ((Number) extraObj).longValue();
                }
                this.variants.add(new ProductVariant(name, colorHex, extra));
            } else if (item instanceof String) {
                this.variants.add(new ProductVariant((String) item, "", 0L));
            }
        }
    }
}
