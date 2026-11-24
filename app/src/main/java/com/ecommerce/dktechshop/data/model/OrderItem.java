package com.ecommerce.dktechshop.data.model;

import java.io.Serializable;

public class OrderItem implements Serializable {
    private String productId = "";
    private String productName = "";
    private String variant = "";
    private long price = 0L;
    private int quantity = 1;

    // Mandatory no-arg constructor for Firestore deserialization
    public OrderItem() {}

    public OrderItem(String productName, String variant, long price, int quantity) {
        this("", productName, variant, price, quantity);
    }

    public OrderItem(String productId, String productName, String variant, long price, int quantity) {
        this.productId = productId != null ? productId : "";
        this.productName = productName != null ? productName : "";
        this.variant = variant != null ? variant : "";
        this.price = price;
        this.quantity = quantity;
    }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getVariant() { return variant; }
    public void setVariant(String variant) { this.variant = variant; }
    public long getPrice() { return price; }
    public void setPrice(long price) { this.price = price; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
