package com.ecommerce.dktechshop.data.repository;

import com.ecommerce.dktechshop.data.model.CartItem;
import com.ecommerce.dktechshop.data.model.Order;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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

        if (order.getRecipientName() == null || order.getRecipientName().trim().isEmpty()) {
            if (user.getDisplayName() != null && !user.getDisplayName().trim().isEmpty()) {
                order.setRecipientName(user.getDisplayName());
            } else {
                order.setRecipientName("Khách hàng");
            }
        }

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
                    if (callback != null) callback.onSuccess(order);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure("Lỗi lưu đơn hàng: " + e.getMessage());
                });
    }

    /**
     * Triển khai listenUserOrders(EventListener): Lắng nghe danh sách đơn hàng lọc theo userId == currentUserId
     * sắp xếp theo ngày đặt (createdAt DESC).
     */
    public ListenerRegistration listenUserOrders(EventListener<QuerySnapshot> listener) {
        FirebaseUser user = mAuth.getCurrentUser();
        String currentUserId = (user != null) ? user.getUid() : "";
        return mFirestore.collection("orders")
                .whereEqualTo("userId", currentUserId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener(listener);
    }

    /**
     * Lắng nghe danh sách đơn hàng người dùng hiện tại lọc theo userId == currentUserId sắp xếp createdAt DESC.
     * Tự động chuyển đổi sang List<Order> và fallback an toàn nếu chưa cấu hình composite index trên Firestore.
     */
    public ListenerRegistration listenUserOrders(OrderListCallback callback) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            if (callback != null) {
                callback.onSuccess(new ArrayList<>());
            }
            return null;
        }

        String currentUserId = user.getUid();
        // Không dùng orderBy trong query Firestore để tránh lỗi thiếu Composite Index (FAILED_PRECONDITION)
        return mFirestore.collection("orders")
                .whereEqualTo("userId", currentUserId)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        fetchOrdersFallback(currentUserId, callback);
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

                    sortOrdersByDateDesc(list);
                    if (callback != null) callback.onSuccess(list);
                });
    }

    /**
     * Fallback tải danh sách đơn hàng từ users/{uid}/orders khi orders bị lỗi truy vấn
     */
    private void fetchOrdersFallback(String uid, OrderListCallback callback) {
        mFirestore.collection("users")
                .document(uid)
                .collection("orders")
                .get()
                .addOnSuccessListener(subSnapshots -> {
                    List<Order> subList = new ArrayList<>();
                    if (subSnapshots != null) {
                        for (DocumentSnapshot doc : subSnapshots.getDocuments()) {
                            Order order = doc.toObject(Order.class);
                            if (order != null) {
                                if (order.getOrderId() == null || order.getOrderId().isEmpty()) {
                                    order.setOrderId(doc.getId());
                                }
                                subList.add(order);
                            }
                        }
                    }

                    sortOrdersByDateDesc(subList);
                    if (callback != null) callback.onSuccess(subList);
                })
                .addOnFailureListener(err -> {
                    if (callback != null) callback.onFailure(err.getMessage());
                });
    }

    private void sortOrdersByDateDesc(List<Order> list) {
        if (list == null || list.size() <= 1) return;
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy • HH:mm", Locale.getDefault());
        list.sort((o1, o2) -> {
            try {
                if (o1.getCreatedAt() != null && o2.getCreatedAt() != null) {
                    Date d1 = sdf.parse(o1.getCreatedAt());
                    Date d2 = sdf.parse(o2.getCreatedAt());
                    if (d1 != null && d2 != null) {
                        return d2.compareTo(d1);
                    }
                }
            } catch (Exception ignored) {}
            String s1 = o1.getCreatedAt() != null ? o1.getCreatedAt() : "";
            String s2 = o2.getCreatedAt() != null ? o2.getCreatedAt() : "";
            return s2.compareTo(s1);
        });
    }

    /**
     * Lắng nghe danh sách đơn hàng thời gian thực từ Firestore users/{uid}/orders (Legacy support)
     */
    public ListenerRegistration listenOrders(OrderListCallback callback) {
        return listenUserOrders(callback);
    }

    /**
     * Lắng nghe một đơn hàng cụ thể theo orderId trong thời gian thực
     */
    public ListenerRegistration listenSingleOrder(String orderId, OrderCallback callback) {
        if (orderId == null || orderId.trim().isEmpty()) {
            if (callback != null) callback.onFailure("Mã đơn hàng không hợp lệ");
            return null;
        }

        return mFirestore.collection("orders").document(orderId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        if (callback != null) callback.onFailure(e.getMessage());
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        Order order = snapshot.toObject(Order.class);
                        if (order != null) {
                            if (order.getOrderId() == null || order.getOrderId().isEmpty()) {
                                order.setOrderId(snapshot.getId());
                            }
                            if (callback != null) callback.onSuccess(order);
                            return;
                        }
                    }

                    if (callback != null) callback.onFailure("Không tìm thấy đơn hàng #" + orderId);
                });
    }

    /**
     * Cập nhật trạng thái đơn hàng (Đang xử lý -> Đang giao -> Đã giao / Hoàn thành -> Đã hủy)
     */
    public void updateOrderStatus(String orderId, String newStatus, ActionCallback callback) {
        FirebaseUser user = mAuth.getCurrentUser();
        String uid = (user != null) ? user.getUid() : "";

        WriteBatch batch = mFirestore.batch();
        DocumentReference rootOrderRef = mFirestore.collection("orders").document(orderId);
        Map<String, Object> update = new HashMap<>();
        update.put("status", newStatus);
        batch.set(rootOrderRef, update, SetOptions.merge());

        if (!uid.isEmpty()) {
            DocumentReference userOrderRef = mFirestore.collection("users")
                    .document(uid)
                    .collection("orders")
                    .document(orderId);
            batch.set(userOrderRef, update, SetOptions.merge());
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure("Lỗi cập nhật đơn hàng: " + e.getMessage());
                });
    }
}
