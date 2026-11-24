package com.ecommerce.dktechshop.data.repository;

import com.ecommerce.dktechshop.data.model.CartItem;
import com.ecommerce.dktechshop.data.model.Order;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderRepository {

    private static OrderRepository instance;
    private final FirebaseAuth mAuth;
    private final FirebaseFirestore mFirestore;

    public interface OrderCallback {
        void onSuccess(Order order);
        void onFailure(String error);
    }

    public interface OrderListCallback {
        void onSuccess(List<Order> orders);
        void onFailure(String error);
    }

    public interface ActionCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public static synchronized OrderRepository getInstance() {
        if (instance == null) {
            instance = new OrderRepository();
        }
        return instance;
    }

    public OrderRepository() {
        this.mAuth = FirebaseAuth.getInstance();
        this.mFirestore = FirebaseFirestore.getInstance();
    }

    /**
     * Triển khai đặt hàng an toàn bằng Firestore WriteBatch:
     * 1. Tạo document mới trong collection orders (và users/{uid}/orders).
     * 2. Giảm số lượng tồn kho stock tương ứng tại collection products.
     * 3. Xóa sạch các món đã đặt trong subcollection users/{uid}/cart.
     */
    public void placeOrderWithBatch(Order order, List<CartItem> cartItems, OrderCallback callback) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            if (callback != null) callback.onFailure("Người dùng chưa đăng nhập");
            return;
        }

        if (cartItems == null || cartItems.isEmpty()) {
            if (callback != null) callback.onFailure("Giỏ hàng đang trống");
            return;
        }

        String uid = user.getUid();
        order.setUserId(uid);

        if (order.getOrderId() == null || order.getOrderId().trim().isEmpty()) {
            order.setOrderId("TG-2026-" + (1000 + (int) (Math.random() * 9000)));
        }

        WriteBatch batch = mFirestore.batch();

        // 1. Tạo document mới trong collection orders
        DocumentReference rootOrderRef = mFirestore.collection("orders").document(order.getOrderId());
        batch.set(rootOrderRef, order);

        DocumentReference userOrderRef = mFirestore.collection("users")
                .document(uid)
                .collection("orders")
                .document(order.getOrderId());
        batch.set(userOrderRef, order);

        // 2. Giảm số lượng tồn kho stock tương ứng tại collection products
        for (CartItem item : cartItems) {
            if (item.getProductId() != null && !item.getProductId().trim().isEmpty()) {
                DocumentReference productDocRef = mFirestore.collection("products").document(item.getProductId());
                Map<String, Object> stockUpdate = new HashMap<>();
                stockUpdate.put("stock", FieldValue.increment(-item.getQuantity()));
                batch.set(productDocRef, stockUpdate, SetOptions.merge());
            }
        }

        // 3. Xóa sạch các món đã đặt trong subcollection users/{uid}/cart
        for (CartItem item : cartItems) {
            if (item.getItemId() != null && !item.getItemId().trim().isEmpty()) {
                DocumentReference cartItemDocRef = mFirestore.collection("users")
                        .document(uid)
                        .collection("cart")
                        .document(item.getItemId());
                batch.delete(cartItemDocRef);
            }
        }

        // Thực thi Batch Write
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    // Cập nhật bộ nhớ đệm MockRepository nếu có
                    MockRepository.getInstance().addOrder(order);

                    if (callback != null) callback.onSuccess(order);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure("Lỗi lưu đơn hàng: " + e.getMessage());
                });
    }

    /**
     * Lắng nghe danh sách đơn hàng thời gian thực từ Firestore users/{uid}/orders
     */
    public ListenerRegistration listenOrders(OrderListCallback callback) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            if (callback != null) callback.onFailure("Người dùng chưa đăng nhập");
            return null;
        }

        return mFirestore.collection("users")
                .document(user.getUid())
                .collection("orders")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        if (callback != null) callback.onFailure(e.getMessage());
                        return;
                    }

                    List<Order> list = new ArrayList<>();
                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            Order order = doc.toObject(Order.class);
                            if (order != null) {
                                if (order.getOrderId() == null || order.getOrderId().isEmpty()) {
                                    order.setOrderId(doc.getId());
                                }
                                list.add(order);
                            }
                        }
                    }

                    if (callback != null) callback.onSuccess(list);
                });
    }

    /**
     * Cập nhật trạng thái đơn hàng (Đang xử lý -> Đang giao -> Hoàn thành)
     */
    public void updateOrderStatus(String orderId, String newStatus, ActionCallback callback) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            if (callback != null) callback.onFailure("Người dùng chưa đăng nhập");
            return;
        }

        WriteBatch batch = mFirestore.batch();
        DocumentReference rootOrderRef = mFirestore.collection("orders").document(orderId);
        batch.update(rootOrderRef, "status", newStatus);

        DocumentReference userOrderRef = mFirestore.collection("users")
                .document(user.getUid())
                .collection("orders")
                .document(orderId);
        batch.update(userOrderRef, "status", newStatus);

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    MockRepository.getInstance().updateOrderStatus(orderId, newStatus);
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e.getMessage());
                });
    }
}
