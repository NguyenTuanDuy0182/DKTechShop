package com.ecommerce.dktechshop.data.model;

import java.io.Serializable;

public class CartItem implements Serializable {
    private String itemId = "";
    private String productId = "";
    private String productName = "";
    private String category = "";
    private long price = 0L;
    private String variant = "";
    private int quantity = 1;
    private String thumbnailUrl = "";

    // Mandatory no-arg constructor for Firestore deserialization
    public CartItem() {}

    public CartItem(String itemId, String productId, String productName, String category, long price, String variant, int quantity) {
        this.itemId = itemId;
        this.productId = productId;
        this.productName = productName;
        this.category = category;
        this.price = price;
        this.variant = variant;
        this.quantity = quantity;
        this.thumbnailUrl = "";
    }

    public CartItem(String itemId, String productId, String productName, String category, long price, String variant, int quantity, String thumbnailUrl) {
        this.itemId = itemId;
        this.productId = productId;
        this.productName = productName;
        this.category = category;
        this.price = price;
        this.variant = variant;
        this.quantity = quantity;
        this.thumbnailUrl = thumbnailUrl != null ? thumbnailUrl : "";
    }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public long getPrice() { return price; }
    public void setPrice(long price) { this.price = price; }
    public String getVariant() { return variant; }
    public void setVariant(String variant) { this.variant = variant; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
}
