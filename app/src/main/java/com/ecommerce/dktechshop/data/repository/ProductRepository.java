package com.ecommerce.dktechshop.data.repository;

import com.ecommerce.dktechshop.data.model.Product;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductRepository {
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
                        Product p = doc.toObject(Product.class);
                        if (p.getId() == null || p.getId().isEmpty()) {
                            p.setId(doc.getId());
                        }
                        list.add(p);
                    }
                    callback.onSuccess(list);
                })
                .addOnFailureListener(callback::onError);
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
                        Product p = doc.toObject(Product.class);
                        if (p.getId() == null || p.getId().isEmpty()) {
                            p.setId(doc.getId());
                        }
                        list.add(p);
                    }
                    callback.onSuccess(list);
                })
                .addOnFailureListener(callback::onError);
    }

    public void getProductById(String productId, Callback<Product> callback) {
        db.collection("products").document(productId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Product p = doc.toObject(Product.class);
                        if (p != null) {
                            if (p.getId() == null || p.getId().isEmpty()) {
                                p.setId(doc.getId());
                            }
                            callback.onSuccess(p);
                            return;
                        }
                    }
                    callback.onError(new Exception("Product not found"));
                })
                .addOnFailureListener(callback::onError);
    }

    public void isFavorite(String productId, Callback<Boolean> callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onSuccess(MockRepository.getInstance().isFavorite(productId));
            return;
        }
        db.collection("users")
                .document(user.getUid())
                .collection("favorites")
                .document(productId)
                .get()
                .addOnSuccessListener(doc -> callback.onSuccess(doc.exists()))
                .addOnFailureListener(callback::onError);
    }

    public void toggleFavorite(String productId, Callback<Boolean> callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            MockRepository.getInstance().toggleFavorite(productId);
            callback.onSuccess(MockRepository.getInstance().isFavorite(productId));
            return;
        }
        DocumentReference docRef = db.collection("users")
                .document(user.getUid())
                .collection("favorites")
                .document(productId);

        docRef.get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                docRef.delete()
                        .addOnSuccessListener(aVoid -> {
                            if (MockRepository.getInstance().isFavorite(productId)) {
                                MockRepository.getInstance().toggleFavorite(productId);
                            }
                            callback.onSuccess(false);
                        })
                        .addOnFailureListener(callback::onError);
            } else {
                Map<String, Object> data = new HashMap<>();
                data.put("productId", productId);
                data.put("timestamp", FieldValue.serverTimestamp());
                docRef.set(data)
                        .addOnSuccessListener(aVoid -> {
                            if (!MockRepository.getInstance().isFavorite(productId)) {
                                MockRepository.getInstance().toggleFavorite(productId);
                            }
                            callback.onSuccess(true);
                        })
                        .addOnFailureListener(callback::onError);
            }
        }).addOnFailureListener(callback::onError);
    }
}
