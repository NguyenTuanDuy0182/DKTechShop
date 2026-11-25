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
import java.util.List;
import java.util.Map;

public class ProductRepository {
    private static final String TAG = "ProductRepository";
    private static ProductRepository instance;
    private final FirebaseFirestore db;

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(Exception e);
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

    public void addReview(Review review, Order order, Callback<Void> callback) {
        if (review == null || review.getProductId() == null || review.getProductId().trim().isEmpty()) {
            if (callback != null) callback.onError(new IllegalArgumentException("Invalid review data"));
            return;
        }

        String productId = review.getProductId();
        if (review.getReviewId() == null || review.getReviewId().trim().isEmpty()) {
            review.setReviewId(db.collection("products").document(productId).collection("reviews").document().getId());
        }

        WriteBatch batch = db.batch();

        // 1. Lưu bài đánh giá vào subcollection reviews của sản phẩm
        DocumentReference reviewRef = db.collection("products")
                .document(productId)
                .collection("reviews")
                .document(review.getReviewId());
        batch.set(reviewRef, review);

        // 2. Cập nhật OrderItem.reviewed = true trong Order nếu có
        if (order != null && order.getOrderId() != null && !order.getOrderId().trim().isEmpty()) {
            if (order.getItems() != null) {
                for (OrderItem item : order.getItems()) {
                    if (productId.equals(item.getProductId()) || review.getProductName().equals(item.getProductName())) {
                        item.setReviewed(true);
                        break;
                    }
                }
            }

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
        }

        // Thực thi batch và sau đó cập nhật rating trung bình
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
                    if (callback != null) callback.onError(e);
                });
    }

    public void checkUserPurchasedProduct(String productId, Callback<Boolean> callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onSuccess(false);
            return;
        }

        String uid = user.getUid();
        db.collection("orders")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    boolean purchased = false;
                    for (QueryDocumentSnapshot doc : querySnapshots) {
                        try {
                            Order o = doc.toObject(Order.class);
                            String status = o.getStatus() != null ? o.getStatus().trim() : "";
                            if ("Đã giao".equalsIgnoreCase(status) || "Hoàn thành".equalsIgnoreCase(status)) {
                                if (o.getItems() != null) {
                                    for (OrderItem item : o.getItems()) {
                                        if (productId.equals(item.getProductId())) {
                                            purchased = true;
                                            break;
                                        }
                                    }
                                }
                            }
                            if (purchased) break;
                        } catch (Exception ignored) {}
                    }
                    callback.onSuccess(purchased);
                })
                .addOnFailureListener(callback::onError);
    }
}
