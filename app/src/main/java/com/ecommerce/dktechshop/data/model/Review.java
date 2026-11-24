package com.ecommerce.dktechshop.data.model;

import java.io.Serializable;

public class Review implements Serializable {
    private String reviewId = "";
    private String productId = "";
    private String productName = "";
    private String orderId = "";
    private String userId = "";
    private String userName = "Khách hàng";
    private float rating = 5.0f;
    private String comment = "";
    private String variant = "";
    private String createdAt = "";

    // Mandatory no-arg constructor for Firestore deserialization
    public Review() {}

    public Review(String reviewId, String productId, String productName, String orderId,
                  String userId, String userName, float rating, String comment,
                  String variant, String createdAt) {
        this.reviewId = reviewId != null ? reviewId : "";
        this.productId = productId != null ? productId : "";
        this.productName = productName != null ? productName : "";
        this.orderId = orderId != null ? orderId : "";
        this.userId = userId != null ? userId : "";
        this.userName = (userName != null && !userName.trim().isEmpty()) ? userName : "Khách hàng";
        this.rating = rating;
        this.comment = comment != null ? comment : "";
        this.variant = variant != null ? variant : "";
        this.createdAt = createdAt != null ? createdAt : "";
    }

    public String getReviewId() { return reviewId; }
    public void setReviewId(String reviewId) { this.reviewId = reviewId; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public String getVariant() { return variant; }
    public void setVariant(String variant) { this.variant = variant; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
