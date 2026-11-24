package com.ecommerce.dktechshop.data.repository;

import android.util.Log;

import com.ecommerce.dktechshop.data.model.Product;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

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
                    if (list.isEmpty()) {
                        list = MockRepository.getInstance().getProducts();
                    }
                    callback.onSuccess(list);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getAllProducts failed", e);
                    callback.onSuccess(MockRepository.getInstance().getProducts());
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
                    if (list.isEmpty()) {
                        list = MockRepository.getInstance().filterProducts(category);
                    }
                    callback.onSuccess(list);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getProductsByCategory failed", e);
                    callback.onSuccess(MockRepository.getInstance().filterProducts(category));
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
                    Product fallback = MockRepository.getInstance().getProductById(productId);
                    if (fallback != null) {
                        callback.onSuccess(fallback);
                    } else {
                        callback.onError(new Exception("Product not found"));
                    }
                })
                .addOnFailureListener(e -> {
                    Product fallback = MockRepository.getInstance().getProductById(productId);
                    if (fallback != null) {
                        callback.onSuccess(fallback);
                    } else {
                        callback.onError(e);
                    }
                });
    }
}
