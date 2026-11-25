package com.ecommerce.dktechshop.data.repository;

import com.ecommerce.dktechshop.data.model.CartItem;
import com.ecommerce.dktechshop.data.model.Order;
import com.ecommerce.dktechshop.data.model.OrderItem;
import com.ecommerce.dktechshop.data.model.Product;
import com.ecommerce.dktechshop.data.model.ProductVariant;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

    public static class StockUpdateResult {
        public final List<Map<String, Object>> serializedVariants;
        public final int totalStock;
        public final int soldCount;
        public final boolean hasVariants;

        public StockUpdateResult(List<Map<String, Object>> serializedVariants, int totalStock, int soldCount, boolean hasVariants) {
            this.serializedVariants = serializedVariants;
            this.totalStock = totalStock;
            this.soldCount = Math.max(0, soldCount);
            this.hasVariants = hasVariants;
        }
    }

    /**
     * Triển khai đặt hàng nguyên tử bằng Firestore Transaction:
     * 1. Đọc toàn bộ sản phẩm liên quan và kiểm tra đủ tồn kho theo từng màu/biến thể.
     * 2. Trừ tồn kho đúng từng màu trong variants và cập nhật lại tổng stock.
     * 3. Tạo document đơn hàng trong orders/{orderId} & users/{uid}/orders/{orderId}.
     * 4. Xóa các món đã đặt khỏi subcollection users/{uid}/cart.
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

        Map<String, Map<String, Integer>> variantDeductions = aggregateCartItemVariantQuantities(cartItems);
        DocumentReference rootOrderRef = mFirestore.collection("orders").document(order.getOrderId());
        DocumentReference userOrderRef = mFirestore.collection("users")
                .document(uid)
                .collection("orders")
                .document(order.getOrderId());

        mFirestore.runTransaction(transaction -> {
            // 1. READ PHASE: Đọc tất cả product docs trước khi thực hiện bất kỳ thao tác ghi nào
            Map<String, StockUpdateResult> stockUpdates = new LinkedHashMap<>();
            for (Map.Entry<String, Map<String, Integer>> entry : variantDeductions.entrySet()) {
                String productId = entry.getKey();
                DocumentReference productDocRef = mFirestore.collection("products").document(productId);
                DocumentSnapshot productSnap = transaction.get(productDocRef);
                if (!productSnap.exists()) {
                    throw new IllegalStateException("Sản phẩm không còn tồn tại trong hệ thống");
                }
                Product product = productSnap.toObject(Product.class);
                if (product == null) {
                    throw new IllegalStateException("Không thể đọc thông tin tồn kho sản phẩm");
                }
                if (product.getId() == null || product.getId().isEmpty()) {
                    product.setId(productSnap.getId());
                }

                Map<String, Integer> negativeDeltas = new LinkedHashMap<>();
                for (Map.Entry<String, Integer> varEntry : entry.getValue().entrySet()) {
                    negativeDeltas.put(varEntry.getKey(), -varEntry.getValue());
                }
                stockUpdates.put(productId, applyVariantStockDeltas(product, negativeDeltas, true));
            }

            // 2. WRITE PHASE: Tạo đơn hàng, cập nhật tồn kho theo từng màu và xóa item đã mua khỏi giỏ
            transaction.set(rootOrderRef, order);
            transaction.set(userOrderRef, order);

            for (Map.Entry<String, StockUpdateResult> entry : stockUpdates.entrySet()) {
                DocumentReference productDocRef = mFirestore.collection("products").document(entry.getKey());
                StockUpdateResult result = entry.getValue();
                Map<String, Object> stockUpdate = new HashMap<>();
                stockUpdate.put("stock", result.totalStock);
                stockUpdate.put("soldCount", result.soldCount);
                if (result.hasVariants && result.serializedVariants != null) {
                    stockUpdate.put("variants", result.serializedVariants);
                }
                transaction.set(productDocRef, stockUpdate, SetOptions.merge());
            }

            for (CartItem item : cartItems) {
                if (item.getItemId() != null && !item.getItemId().trim().isEmpty()) {
                    DocumentReference cartItemDocRef = mFirestore.collection("users")
                            .document(uid)
                            .collection("cart")
                            .document(item.getItemId());
                    transaction.delete(cartItemDocRef);
                }
            }

            return order;
        }).addOnSuccessListener(savedOrder -> {
            if (callback != null) callback.onSuccess(savedOrder);
        }).addOnFailureListener(e -> {
            String msg = extractReadableError(e, "Lỗi lưu đơn hàng");
            if (callback != null) callback.onFailure(msg);
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

    public static Map<String, Long> aggregateCartItemQuantities(List<CartItem> items) {
        Map<String, Long> totals = new HashMap<>();
        if (items == null) return totals;
        for (CartItem item : items) {
            if (item != null && item.getProductId() != null && !item.getProductId().trim().isEmpty() && item.getQuantity() > 0) {
                String pid = item.getProductId().trim();
                totals.put(pid, totals.getOrDefault(pid, 0L) + item.getQuantity());
            }
        }
        return totals;
    }

    public static Map<String, Long> aggregateOrderItemQuantities(List<OrderItem> items) {
        Map<String, Long> totals = new HashMap<>();
        if (items == null) return totals;
        for (OrderItem item : items) {
            if (item != null && item.getProductId() != null && !item.getProductId().trim().isEmpty() && item.getQuantity() > 0) {
                String pid = item.getProductId().trim();
                totals.put(pid, totals.getOrDefault(pid, 0L) + item.getQuantity());
            }
        }
        return totals;
    }

    public static Map<String, Map<String, Integer>> aggregateCartItemVariantQuantities(List<CartItem> items) {
        Map<String, Map<String, Integer>> result = new LinkedHashMap<>();
        if (items == null) return result;
        for (CartItem item : items) {
            if (item != null && item.getProductId() != null && !item.getProductId().trim().isEmpty() && item.getQuantity() > 0) {
                String pid = item.getProductId().trim();
                String variant = item.getVariant() != null ? item.getVariant().trim() : "";
                Map<String, Integer> variantMap = result.computeIfAbsent(pid, k -> new LinkedHashMap<>());
                variantMap.put(variant, variantMap.getOrDefault(variant, 0) + item.getQuantity());
            }
        }
        return result;
    }

    public static Map<String, Map<String, Integer>> aggregateOrderItemVariantQuantities(List<OrderItem> items) {
        Map<String, Map<String, Integer>> result = new LinkedHashMap<>();
        if (items == null) return result;
        for (OrderItem item : items) {
            if (item != null && item.getProductId() != null && !item.getProductId().trim().isEmpty() && item.getQuantity() > 0) {
                String pid = item.getProductId().trim();
                String variant = item.getVariant() != null ? item.getVariant().trim() : "";
                Map<String, Integer> variantMap = result.computeIfAbsent(pid, k -> new LinkedHashMap<>());
                variantMap.put(variant, variantMap.getOrDefault(variant, 0) + item.getQuantity());
            }
        }
        return result;
    }

    /**
     * Tính toán cập nhật tồn kho theo từng màu/biến thể và tổng stock.
     * - delta < 0: trừ kho khi đặt hàng (khi requireSufficientStock = true sẽ báo lỗi nếu màu đó không đủ hàng).
     * - delta > 0: hoàn kho về đúng màu tương ứng khi hủy đơn hàng.
     */
    public static StockUpdateResult applyVariantStockDeltas(Product product,
                                                            Map<String, Integer> variantDeltas,
                                                            boolean requireSufficientStock) {
        if (product == null) {
            throw new IllegalArgumentException("Sản phẩm không hợp lệ");
        }
        String productName = (product.getName() != null && !product.getName().trim().isEmpty())
                ? product.getName().trim() : "sản phẩm";

        if (product.hasDefinedVariants() && product.hasExplicitVariantStock()) {
            List<ProductVariant> copied = new ArrayList<>();
            for (ProductVariant v : product.getVariants()) {
                if (v != null) {
                    copied.add(new ProductVariant(v.getName(), v.getColorHex(), v.getExtraPrice(), v.getStock()));
                }
            }

            int totalDelta = 0;
            if (variantDeltas != null) {
                for (Map.Entry<String, Integer> entry : variantDeltas.entrySet()) {
                    int delta = entry.getValue() != null ? entry.getValue() : 0;
                    if (delta == 0) continue;
                    totalDelta += delta;

                    String requestedVariant = entry.getKey() != null ? entry.getKey().trim() : "";
                    ProductVariant matched = null;
                    for (ProductVariant v : copied) {
                        if (v.getName() != null && v.getName().trim().equalsIgnoreCase(requestedVariant)) {
                            matched = v;
                            break;
                        }
                    }

                    if (matched == null && !copied.isEmpty()
                            && (requestedVariant.isEmpty()
                            || "Mặc định".equalsIgnoreCase(requestedVariant)
                            || !requireSufficientStock)) {
                        matched = copied.get(0);
                    }

                    if (matched == null) {
                        throw new IllegalStateException("Phân loại \"" + requestedVariant + "\" của " + productName + " không còn tồn tại");
                    }

                    int currentStock = Math.max(0, matched.getStock());
                    int updatedStock = currentStock + delta;
                    if (requireSufficientStock && updatedStock < 0) {
                        if (currentStock <= 0) {
                            throw new IllegalStateException("Sản phẩm " + productName + " (" + matched.getName() + ") hiện đã hết hàng");
                        }
                        throw new IllegalStateException("Sản phẩm " + productName + " (" + matched.getName() + ") chỉ còn " + currentStock + " sản phẩm trong kho");
                    }
                    matched.setStock(Math.max(0, updatedStock));
                }
            }

            int totalStock = 0;
            List<Map<String, Object>> serialized = new ArrayList<>();
            for (ProductVariant v : copied) {
                int safeStock = Math.max(0, v.getStock());
                totalStock += safeStock;
                Map<String, Object> map = new HashMap<>();
                map.put("name", v.getName());
                map.put("colorHex", v.getColorHex());
                map.put("extraPrice", v.getExtraPrice());
                map.put("stock", safeStock);
                serialized.add(map);
            }
            int updatedSoldCount = Math.max(0, product.getSoldCount() - totalDelta);
            return new StockUpdateResult(serialized, totalStock, updatedSoldCount, true);
        }

        int totalDelta = 0;
        if (variantDeltas != null) {
            for (Integer d : variantDeltas.values()) {
                if (d != null) totalDelta += d;
            }
        }
        int currentStock = Math.max(0, product.getStock());
        int updatedStock = currentStock + totalDelta;
        if (requireSufficientStock && updatedStock < 0) {
            if (currentStock <= 0) {
                throw new IllegalStateException("Sản phẩm " + productName + " hiện đã hết hàng");
            }
            throw new IllegalStateException("Sản phẩm " + productName + " chỉ còn " + currentStock + " sản phẩm trong kho");
        }
        int updatedSoldCount = Math.max(0, product.getSoldCount() - totalDelta);
        return new StockUpdateResult(null, Math.max(0, updatedStock), updatedSoldCount, false);
    }

    private static String extractReadableError(Exception e, String fallbackPrefix) {
        if (e == null) return fallbackPrefix;
        if (e instanceof IllegalStateException && e.getMessage() != null && !e.getMessage().trim().isEmpty()) {
            return e.getMessage();
        }
        if (e.getCause() instanceof IllegalStateException
                && e.getCause().getMessage() != null
                && !e.getCause().getMessage().trim().isEmpty()) {
            return e.getCause().getMessage();
        }
        if (e.getMessage() != null && !e.getMessage().trim().isEmpty()) {
            return fallbackPrefix + ": " + e.getMessage();
        }
        return fallbackPrefix;
    }

    /**
     * Cập nhật trạng thái đơn hàng (Đang xử lý -> Đang giao -> Đã giao / Hoàn thành -> Đã hủy).
     * Khi hủy đơn sẽ dùng Firestore Transaction để hoàn trả tồn kho (stock) theo đúng màu an toàn, tránh cộng trùng.
     */
    public void updateOrderStatus(String orderId, String newStatus, ActionCallback callback) {
        if (newStatus != null && "Đã hủy".equalsIgnoreCase(newStatus.trim())) {
            cancelOrderWithStockRestore(orderId, callback);
            return;
        }

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

    /**
     * Hủy đơn hàng nguyên tử bằng Firestore Transaction:
     * 1. Đọc đơn hàng và kiểm tra trạng thái chưa bị hủy và chưa giao.
     * 2. Đọc tất cả products/{productId} liên quan (tất cả lệnh đọc trước lệnh ghi).
     * 3. Hoàn trả tồn kho về đúng từng màu trong variants và cập nhật lại tổng stock.
     * 4. Cập nhật status = "Đã hủy" ở cả orders/{orderId} và users/{uid}/orders/{orderId}.
     */
    private void cancelOrderWithStockRestore(String orderId, ActionCallback callback) {
        if (orderId == null || orderId.trim().isEmpty()) {
            if (callback != null) callback.onFailure("Mã đơn hàng không hợp lệ");
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        String uid = (user != null) ? user.getUid() : "";
        DocumentReference rootOrderRef = mFirestore.collection("orders").document(orderId);

        mFirestore.runTransaction(transaction -> {
            // 1. READ PHASE: Đọc đơn hàng
            DocumentSnapshot snapshot = transaction.get(rootOrderRef);
            if (!snapshot.exists() && !uid.isEmpty()) {
                DocumentReference userOrderRef = mFirestore.collection("users")
                        .document(uid)
                        .collection("orders")
                        .document(orderId);
                snapshot = transaction.get(userOrderRef);
            }

            if (!snapshot.exists()) {
                throw new IllegalStateException("Không tìm thấy đơn hàng #" + orderId);
            }

            Order existing = snapshot.toObject(Order.class);
            String currentStatus = (existing != null && existing.getStatus() != null)
                    ? existing.getStatus().trim() : "";

            if ("Đã hủy".equalsIgnoreCase(currentStatus)) {
                throw new IllegalStateException("Đơn hàng này đã được hủy trước đó");
            }
            if ("Đã giao".equalsIgnoreCase(currentStatus) || "Hoàn thành".equalsIgnoreCase(currentStatus)) {
                throw new IllegalStateException("Không thể hủy đơn hàng đã giao");
            }

            // 2. READ PHASE: Đọc tất cả sản phẩm cần hoàn kho trước khi ghi
            Map<String, StockUpdateResult> stockRestores = new LinkedHashMap<>();
            if (existing != null && existing.getItems() != null) {
                Map<String, Map<String, Integer>> restoreMap = aggregateOrderItemVariantQuantities(existing.getItems());
                for (Map.Entry<String, Map<String, Integer>> entry : restoreMap.entrySet()) {
                    String productId = entry.getKey();
                    DocumentReference productDocRef = mFirestore.collection("products").document(productId);
                    DocumentSnapshot prodSnap = transaction.get(productDocRef);
                    if (prodSnap.exists()) {
                        Product product = prodSnap.toObject(Product.class);
                        if (product != null) {
                            if (product.getId() == null || product.getId().isEmpty()) {
                                product.setId(prodSnap.getId());
                            }
                            stockRestores.put(productId, applyVariantStockDeltas(product, entry.getValue(), false));
                        }
                    }
                }
            }

            // 3. WRITE PHASE: Cập nhật trạng thái đơn và hoàn trả tồn kho đúng từng màu
            Map<String, Object> update = new HashMap<>();
            update.put("status", "Đã hủy");
            transaction.set(rootOrderRef, update, SetOptions.merge());

            String orderUid = !uid.isEmpty() ? uid
                    : (existing != null && existing.getUserId() != null ? existing.getUserId() : "");
            if (!orderUid.isEmpty()) {
                DocumentReference userOrderRef = mFirestore.collection("users")
                        .document(orderUid)
                        .collection("orders")
                        .document(orderId);
                transaction.set(userOrderRef, update, SetOptions.merge());
            }

            for (Map.Entry<String, StockUpdateResult> entry : stockRestores.entrySet()) {
                DocumentReference productDocRef = mFirestore.collection("products").document(entry.getKey());
                StockUpdateResult result = entry.getValue();
                Map<String, Object> stockRestore = new HashMap<>();
                stockRestore.put("stock", result.totalStock);
                stockRestore.put("soldCount", result.soldCount);
                if (result.hasVariants && result.serializedVariants != null) {
                    stockRestore.put("variants", result.serializedVariants);
                }
                transaction.set(productDocRef, stockRestore, SetOptions.merge());
            }
            return null;
        }).addOnSuccessListener(aVoid -> {
            if (callback != null) callback.onSuccess();
        }).addOnFailureListener(e -> {
            String msg = extractReadableError(e, "Lỗi hủy đơn hàng");
            if (callback != null) callback.onFailure(msg);
        });
    }
}
