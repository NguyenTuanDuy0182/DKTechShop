package com.ecommerce.dktechshop.data.repository;

import android.util.Log;

import com.ecommerce.dktechshop.data.model.Order;
import com.ecommerce.dktechshop.data.model.OrderItem;
import com.ecommerce.dktechshop.data.model.Product;
import com.ecommerce.dktechshop.data.model.Review;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProductRepository {
    private static final String TAG = "ProductRepository";
    private static ProductRepository instance;
    private final FirebaseFirestore db;

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }

    public static class ReviewEligibility {
        public enum Status {
            ELIGIBLE,
            NOT_DELIVERED,
            ALREADY_REVIEWED
        }

        public final Status status;
        public final Order eligibleOrder;
        public final OrderItem eligibleItem;

        public ReviewEligibility(Status status, Order eligibleOrder, OrderItem eligibleItem) {
            this.status = status;
            this.eligibleOrder = eligibleOrder;
            this.eligibleItem = eligibleItem;
        }
    }

    private ProductRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized ProductRepository getInstance() {
        if (instance == null) {
            instance = new ProductRepository();
        }
        return instance;
    }

    public void getAllProducts(Callback<List<Product>> callback) {
        db.collection("products")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Product> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            Product p = doc.toObject(Product.class);
                            if (p != null) {
                                if (p.getId() == null || p.getId().isEmpty()) {
                                    p.setId(doc.getId());
                                }
                                list.add(p);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing product: " + doc.getId(), e);
                        }
                    }
                    callback.onSuccess(list);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getAllProducts failed", e);
                    callback.onError(e);
                });
    }

    public void getProductsByCategory(String category, Callback<List<Product>> callback) {
        if (category == null || category.isEmpty() || "all".equalsIgnoreCase(category)) {
            getAllProducts(callback);
            return;
        }

        Query query;
        if ("under35".equalsIgnoreCase(category)) {
            query = db.collection("products").whereLessThan("price", 3500000L);
        } else {
            query = db.collection("products").whereEqualTo("category", category);
        }

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Product> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            Product p = doc.toObject(Product.class);
                            if (p != null) {
                                if (p.getId() == null || p.getId().isEmpty()) {
                                    p.setId(doc.getId());
                                }
                                list.add(p);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing product: " + doc.getId(), e);
                        }
                    }
                    callback.onSuccess(list);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getProductsByCategory failed", e);
                    callback.onError(e);
                });
    }

    public void getProductById(String productId, Callback<Product> callback) {
        db.collection("products").document(productId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        try {
                            Product p = doc.toObject(Product.class);
                            if (p != null) {
                                if (p.getId() == null || p.getId().isEmpty()) {
                                    p.setId(doc.getId());
                                }
                                callback.onSuccess(p);
                                return;
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing product " + productId, e);
                        }
                    }
                    callback.onError(new Exception("Không tìm thấy sản phẩm"));
                })
                .addOnFailureListener(callback::onError);
    }

    public void getReviews(String productId, Callback<List<Review>> callback) {
        if (productId == null || productId.trim().isEmpty()) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        db.collection("products")
                .document(productId)
                .collection("reviews")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    List<Review> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshots) {
                        try {
                            Review r = doc.toObject(Review.class);
                            if (r != null) {
                                if (r.getReviewId() == null || r.getReviewId().isEmpty()) {
                                    r.setReviewId(doc.getId());
                                }
                                list.add(r);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing review: " + doc.getId(), e);
                        }
                    }
                    callback.onSuccess(list);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getReviews failed", e);
                    callback.onError(e);
                });
    }

    public static boolean isDeliveredStatus(String status) {
        if (status == null) return false;
        String s = status.trim();
        return "Đã giao".equalsIgnoreCase(s) || "Hoàn thành".equalsIgnoreCase(s);
    }

    public static String generateReviewId(String orderId, String productId, String variant) {
        String safeOrder = (orderId != null && !orderId.trim().isEmpty())
                ? orderId.trim().replaceAll("[^a-zA-Z0-9_-]", "_")
                : "order";
        String safeProd = (productId != null && !productId.trim().isEmpty())
                ? productId.trim().replaceAll("[^a-zA-Z0-9_-]", "_")
                : "prod";
        String safeVariant = (variant != null && !variant.trim().isEmpty())
                ? variant.trim().replaceAll("[^a-zA-Z0-9_-]", "_")
                : "default";
        return safeOrder + "_" + safeProd + "_" + safeVariant;
    }

    public void addReview(Review review, Order order, Callback<Void> callback) {
        if (review == null || review.getProductId() == null || review.getProductId().trim().isEmpty()) {
            if (callback != null) callback.onError(new IllegalArgumentException("Dữ liệu đánh giá không hợp lệ"));
            return;
        }

        if (order == null || order.getOrderId() == null || order.getOrderId().trim().isEmpty()) {
            if (callback != null) callback.onError(new IllegalStateException("Chỉ có thể đánh giá sản phẩm từ đơn hàng đã giao"));
            return;
        }

        if (!isDeliveredStatus(order.getStatus())) {
            if (callback != null) callback.onError(new IllegalStateException("Chỉ đơn hàng đã giao mới được phép đánh giá"));
            return;
        }

        String productId = review.getProductId().trim();
        String targetVariant = review.getVariant() != null ? review.getVariant().trim() : "";

        // Tìm đúng OrderItem chưa được đánh giá trong đơn hàng này
        OrderItem matchedItem = null;
        if (order.getItems() != null) {
            // Ưu tiên khớp cả productId và variant
            for (OrderItem item : order.getItems()) {
                boolean sameProduct = productId.equals(item.getProductId())
                        || (review.getProductName() != null && review.getProductName().equals(item.getProductName()));
                String itemVariant = item.getVariant() != null ? item.getVariant().trim() : "";
                boolean sameVariant = targetVariant.isEmpty() || targetVariant.equalsIgnoreCase(itemVariant);
                if (sameProduct && sameVariant && !item.isReviewed()) {
                    matchedItem = item;
                    break;
                }
            }
            // Fallback nếu variant không truyền lên
            if (matchedItem == null) {
                for (OrderItem item : order.getItems()) {
                    boolean sameProduct = productId.equals(item.getProductId())
                            || (review.getProductName() != null && review.getProductName().equals(item.getProductName()));
                    if (sameProduct && !item.isReviewed()) {
                        matchedItem = item;
                        break;
                    }
                }
            }
        }

        if (matchedItem == null) {
            if (callback != null) callback.onError(new IllegalStateException("Bạn đã đánh giá sản phẩm này cho đơn hàng này rồi"));
            return;
        }

        final OrderItem finalMatchedItem = matchedItem;
        String finalVariant = finalMatchedItem.getVariant() != null ? finalMatchedItem.getVariant() : targetVariant;
        review.setOrderId(order.getOrderId());
        review.setVariant(finalVariant);
        review.setReviewId(generateReviewId(order.getOrderId(), productId, finalVariant));

        DocumentReference reviewRef = db.collection("products")
                .document(productId)
                .collection("reviews")
                .document(review.getReviewId());

        // Kiểm tra chắc chắn trên Firestore rằng đơn hàng + sản phẩm này chưa từng được đánh giá
        reviewRef.get()
                .addOnSuccessListener(existingDoc -> {
                    if (existingDoc != null && existingDoc.exists()) {
                        if (callback != null) {
                            callback.onError(new IllegalStateException("Bạn đã đánh giá sản phẩm này cho đơn hàng này rồi"));
                        }
                        return;
                    }

                    finalMatchedItem.setReviewed(true);

                    WriteBatch batch = db.batch();
                    batch.set(reviewRef, review);

                    DocumentReference rootOrderRef = db.collection("orders").document(order.getOrderId());
                    batch.set(rootOrderRef, order, SetOptions.merge());

                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user != null) {
                        DocumentReference userOrderRef = db.collection("users")
                                .document(user.getUid())
                                .collection("orders")
                                .document(order.getOrderId());
                        batch.set(userOrderRef, order, SetOptions.merge());
                    }

                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                // Cập nhật rating và reviewCount trên document sản phẩm
                                db.collection("products").document(productId).get()
                                        .addOnSuccessListener(productDoc -> {
                                            if (productDoc.exists()) {
                                                Product p = productDoc.toObject(Product.class);
                                                if (p != null) {
                                                    int count = p.getReviewCount() + 1;
                                                    double newRating = Math.round((((p.getRating() * p.getReviewCount()) + review.getRating()) / count) * 10.0) / 10.0;
                                                    db.collection("products").document(productId)
                                                            .update("reviewCount", count, "rating", newRating);
                                                }
                                            }
                                        });

                                if (callback != null) callback.onSuccess(null);
                            })
                            .addOnFailureListener(e -> {
                                finalMatchedItem.setReviewed(false);
                                if (callback != null) callback.onError(e);
                            });
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }

    public static ReviewEligibility evaluateReviewEligibility(List<Order> userOrders,
                                                              List<Review> existingUserReviews,
                                                              String productId) {
        if (productId == null || productId.trim().isEmpty() || userOrders == null || userOrders.isEmpty()) {
            return new ReviewEligibility(ReviewEligibility.Status.NOT_DELIVERED, null, null);
        }

        Set<String> reviewedReviewIds = new HashSet<>();
        Set<String> reviewedOrderIdsNoVariant = new HashSet<>();
        int legacyUnlinkedReviews = 0;

        if (existingUserReviews != null) {
            for (Review r : existingUserReviews) {
                if (r == null) continue;
                String rOrderId = r.getOrderId() != null ? r.getOrderId().trim() : "";
                String rVariant = r.getVariant() != null ? r.getVariant().trim() : "";
                if (!rOrderId.isEmpty()) {
                    reviewedReviewIds.add(generateReviewId(rOrderId, productId, rVariant));
                    if (rVariant.isEmpty()) {
                        reviewedOrderIdsNoVariant.add(rOrderId);
                    }
                } else {
                    legacyUnlinkedReviews++;
                }
            }
        }

        boolean hasDeliveredItem = false;
        List<Order> candidateOrders = new ArrayList<>();
        List<OrderItem> candidateItems = new ArrayList<>();

        for (Order o : userOrders) {
            if (o == null || !isDeliveredStatus(o.getStatus()) || o.getItems() == null) continue;
            for (OrderItem item : o.getItems()) {
                if (item == null || !productId.equals(item.getProductId())) continue;
                hasDeliveredItem = true;

                String expectedId = generateReviewId(o.getOrderId(), productId, item.getVariant());
                boolean alreadyReviewed = item.isReviewed()
                        || reviewedReviewIds.contains(expectedId)
                        || (o.getOrderId() != null && reviewedOrderIdsNoVariant.contains(o.getOrderId().trim()));
                if (!alreadyReviewed) {
                    candidateOrders.add(o);
                    candidateItems.add(item);
                }
            }
        }

        if (!hasDeliveredItem) {
            return new ReviewEligibility(ReviewEligibility.Status.NOT_DELIVERED, null, null);
        }

        // Trừ đi số lượt đánh giá cũ (nếu có review trước đây chưa gắn orderId)
        if (candidateOrders.size() > legacyUnlinkedReviews) {
            return new ReviewEligibility(
                    ReviewEligibility.Status.ELIGIBLE,
                    candidateOrders.get(0),
                    candidateItems.get(0)
            );
        }

        return new ReviewEligibility(ReviewEligibility.Status.ALREADY_REVIEWED, null, null);
    }

    public void checkReviewEligibility(String productId, Callback<ReviewEligibility> callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || productId == null || productId.trim().isEmpty()) {
            callback.onSuccess(new ReviewEligibility(ReviewEligibility.Status.NOT_DELIVERED, null, null));
            return;
        }

        String uid = user.getUid();
        db.collection("orders")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(orderSnapshots -> {
                    List<Order> userOrders = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : orderSnapshots) {
                        try {
                            Order o = doc.toObject(Order.class);
                            if (o != null) {
                                if (o.getOrderId() == null || o.getOrderId().isEmpty()) {
                                    o.setOrderId(doc.getId());
                                }
                                userOrders.add(o);
                            }
                        } catch (Exception ignored) {}
                    }

                    db.collection("products")
                            .document(productId)
                            .collection("reviews")
                            .whereEqualTo("userId", uid)
                            .get()
                            .addOnSuccessListener(reviewSnapshots -> {
                                List<Review> existingReviews = new ArrayList<>();
                                for (QueryDocumentSnapshot rDoc : reviewSnapshots) {
                                    try {
                                        Review r = rDoc.toObject(Review.class);
                                        if (r != null) existingReviews.add(r);
                                    } catch (Exception ignored) {}
                                }
                                callback.onSuccess(evaluateReviewEligibility(userOrders, existingReviews, productId));
                            })
                            .addOnFailureListener(e ->
                                    callback.onSuccess(evaluateReviewEligibility(userOrders, null, productId))
                            );
                })
                .addOnFailureListener(callback::onError);
    }

    public void checkUserPurchasedProduct(String productId, Callback<Boolean> callback) {
        checkReviewEligibility(productId, new Callback<ReviewEligibility>() {
            @Override
            public void onSuccess(ReviewEligibility result) {
                callback.onSuccess(result != null && result.status == ReviewEligibility.Status.ELIGIBLE);
            }

            @Override
            public void onError(Exception e) {
                callback.onError(e);
            }
        });
    }
}
