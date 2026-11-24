package com.ecommerce.dktechshop.data.repository;

import com.ecommerce.dktechshop.data.model.Product;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

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
}
