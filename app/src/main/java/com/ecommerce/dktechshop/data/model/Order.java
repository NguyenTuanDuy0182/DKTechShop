package com.ecommerce.dktechshop.data.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Order implements Serializable {
    private String orderId = "";
    private String userId = "";
    private String createdAt = "";
    private String status = "Đang xử lý";
    private String paymentMethod = "COD";
    private String shippingAddress = "";
    private long totalAmount = 0L;
    private List<OrderItem> items = new ArrayList<>();

    // Mandatory no-arg constructor for Firestore deserialization
    public Order() {}

    public Order(String orderId, String createdAt, String status, String paymentMethod,
                 String shippingAddress, long totalAmount, List<OrderItem> items) {
        this(orderId, "", createdAt, status, paymentMethod, shippingAddress, totalAmount, items);
    }

    public Order(String orderId, String userId, String createdAt, String status, String paymentMethod,
                 String shippingAddress, long totalAmount, List<OrderItem> items) {
        this.orderId = orderId;
        this.userId = userId != null ? userId : "";
        this.createdAt = createdAt;
        this.status = status;
        this.paymentMethod = paymentMethod;
        this.shippingAddress = shippingAddress;
        this.totalAmount = totalAmount;
        this.items = items != null ? items : new ArrayList<>();
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }
    public long getTotalAmount() { return totalAmount; }
    public void setTotalAmount(long totalAmount) { this.totalAmount = totalAmount; }
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
}
