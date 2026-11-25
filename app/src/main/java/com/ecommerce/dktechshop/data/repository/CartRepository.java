package com.ecommerce.dktechshop.data.repository;

import androidx.annotation.Nullable;

import com.ecommerce.dktechshop.data.model.CartItem;
import com.ecommerce.dktechshop.data.model.Product;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

public class CartRepository {

    private static CartRepository instance;
    private final FirebaseAuth mAuth;
    private final FirebaseFirestore mFirestore;

    public interface CartListener {
        void onCartChanged(List<CartItem> items);
        void onError(String error);
    }

    public interface ActionCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public static synchronized CartRepository getInstance() {
        if (instance == null) {
            instance = new CartRepository();
        }
        return instance;
    }

    public CartRepository() {
        this.mAuth = FirebaseAuth.getInstance();
        this.mFirestore = FirebaseFirestore.getInstance();
    }

    @Nullable
    private String getCurrentUserId() {
        FirebaseUser user = mAuth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    private CollectionReference getCartCollection(String uid) {
        return mFirestore.collection("users").document(uid).collection("cart");
    }

    /**
     * Sinh Document ID chuẩn hóa từ productId và variant
     */
    public static String generateItemId(String productId, String variant) {
        String safeVariant = (variant != null && !variant.trim().isEmpty())
                ? variant.trim().replaceAll("[^a-zA-Z0-9_-]", "_")
                : "default";
        return productId + "_" + safeVariant;
    }

    /**
     * Lắng nghe giỏ hàng thời gian thực qua addSnapshotListener tại path users/{uid}/cart.
     */
    public ListenerRegistration listenCartItems(CartListener listener) {
        String uid = getCurrentUserId();
        if (uid == null) {
            if (listener != null) {
                listener.onError("Người dùng chưa đăng nhập");
            }
            return null;
        }

        return getCartCollection(uid).addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                if (listener != null) {
                    listener.onError(e.getMessage());
                }
                return;
            }

            List<CartItem> list = new ArrayList<>();
            if (snapshots != null) {
                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    CartItem item = doc.toObject(CartItem.class);
                    if (item != null) {
                        if (item.getItemId() == null || item.getItemId().isEmpty()) {
                            item.setItemId(doc.getId());
                        }
                        list.add(item);
                    }
                }
            }

            if (listener != null) {
                listener.onCartChanged(list);
            }
        });
    }

    /**
     * Thêm hoặc cập nhật item trong giỏ hàng (upsertCartItem).
     * Nếu sản phẩm cùng biến thể đã tồn tại -> cộng dồn số lượng.
     */
    public void upsertCartItem(CartItem item, ActionCallback callback) {
        String uid = getCurrentUserId();
        if (uid == null) {
            if (callback != null) callback.onFailure("Người dùng chưa đăng nhập");
            return;
        }

        if (item.getItemId() == null || item.getItemId().trim().isEmpty()) {
            item.setItemId(generateItemId(item.getProductId(), item.getVariant()));
        }

        DocumentReference docRef = getCartCollection(uid).document(item.getItemId());
        String productId = item.getProductId() != null ? item.getProductId().trim() : "";
        String variantName = item.getVariant() != null ? item.getVariant().trim() : "";

        mFirestore.runTransaction(transaction -> {
            // READ PHASE: đọc cả cart item và product trước khi ghi
            DocumentSnapshot snapshot = transaction.get(docRef);
            DocumentSnapshot prodSnap = !productId.isEmpty()
                    ? transaction.get(mFirestore.collection("products").document(productId))
                    : null;

            int currentQty = 0;
            if (snapshot.exists()) {
                Long existingQty = snapshot.getLong("quantity");
                currentQty = existingQty != null ? existingQty.intValue() : 0;
            }
            int updatedQty = currentQty + item.getQuantity();

            if (prodSnap != null && prodSnap.exists()) {
                Product p = prodSnap.toObject(Product.class);
                if (p != null) {
                    int maxStock = p.resolveVariantStock(variantName);
                    String label = variantName.isEmpty() ? p.getName() : "màu " + variantName;
                    if (maxStock <= 0) {
                        throw new IllegalStateException("Sản phẩm (" + label + ") hiện đã hết hàng");
                    }
                    if (updatedQty > maxStock) {
                        throw new IllegalStateException("Sản phẩm (" + label + ") chỉ còn " + maxStock + " trong kho");
                    }
                }
            }

            // WRITE PHASE
            if (snapshot.exists()) {
                transaction.update(docRef, "quantity", updatedQty);
            } else {
                transaction.set(docRef, item);
            }
            return null;
        }).addOnSuccessListener(aVoid -> {
            if (callback != null) callback.onSuccess();
        }).addOnFailureListener(e -> {
            String msg = (e != null && e.getMessage() != null) ? e.getMessage() : "Không thể thêm vào giỏ hàng";
            if (e != null && e.getCause() instanceof IllegalStateException && e.getCause().getMessage() != null) {
                msg = e.getCause().getMessage();
            }
            if (callback != null) callback.onFailure(msg);
        });
    }

    /**
     * Cập nhật số lượng item với delta (+1 hoặc -1).
     * Nếu tăng số lượng sẽ kiểm tra tồn kho theo đúng màu trên Firestore.
     * Nếu số lượng mới <= 0 -> tự động xóa document khỏi giỏ.
     */
    public void updateQuantity(String itemId, int delta, ActionCallback callback) {
        String uid = getCurrentUserId();
        if (uid == null) {
            if (callback != null) callback.onFailure("Người dùng chưa đăng nhập");
            return;
        }

        DocumentReference docRef = getCartCollection(uid).document(itemId);

        mFirestore.runTransaction(transaction -> {
            // READ PHASE
            DocumentSnapshot snapshot = transaction.get(docRef);
            if (!snapshot.exists()) {
                return false;
            }

            Long existingQty = snapshot.getLong("quantity");
            int currentQty = existingQty != null ? existingQty.intValue() : 1;
            int newQty = currentQty + delta;

            if (delta > 0) {
                String productId = snapshot.getString("productId");
                String variantName = snapshot.getString("variant");
                if (productId != null && !productId.trim().isEmpty()) {
                    DocumentSnapshot prodSnap = transaction.get(
                            mFirestore.collection("products").document(productId.trim()));
                    if (prodSnap.exists()) {
                        Product p = prodSnap.toObject(Product.class);
                        if (p != null) {
                            int maxStock = p.resolveVariantStock(variantName);
                            String label = (variantName != null && !variantName.trim().isEmpty())
                                    ? "màu " + variantName.trim() : p.getName();
                            if (maxStock <= 0) {
                                throw new IllegalStateException("Sản phẩm (" + label + ") hiện đã hết hàng");
                            }
                            if (newQty > maxStock) {
                                throw new IllegalStateException("Sản phẩm (" + label + ") chỉ còn " + maxStock + " trong kho");
                            }
                        }
                    }
                }
            }

            // WRITE PHASE
            if (newQty <= 0) {
                transaction.delete(docRef);
            } else {
                transaction.update(docRef, "quantity", newQty);
            }
            return true;
        }).addOnSuccessListener(result -> {
            if (callback != null) callback.onSuccess();
        }).addOnFailureListener(e -> {
            String msg = (e != null && e.getMessage() != null) ? e.getMessage() : "Không thể cập nhật số lượng";
            if (e != null && e.getCause() instanceof IllegalStateException && e.getCause().getMessage() != null) {
                msg = e.getCause().getMessage();
            }
            if (callback != null) callback.onFailure(msg);
        });
    }

    /**
     * Xóa trực tiếp item khỏi giỏ hàng.
     */
    public void deleteCartItem(String itemId, ActionCallback callback) {
        String uid = getCurrentUserId();
        if (uid == null) {
            if (callback != null) callback.onFailure("Người dùng chưa đăng nhập");
            return;
        }

        getCartCollection(uid).document(itemId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e.getMessage());
                });
    }

    /**
     * Xóa toàn bộ giỏ hàng (ví dụ sau khi đặt hàng thành công).
     */
    public void clearCart(ActionCallback callback) {
        String uid = getCurrentUserId();
        if (uid == null) {
            if (callback != null) callback.onFailure("Người dùng chưa đăng nhập");
            return;
        }

        getCartCollection(uid).get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots.isEmpty()) {
                        if (callback != null) callback.onSuccess();
                        return;
                    }

                    WriteBatch batch = mFirestore.batch();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        batch.delete(doc.getReference());
                    }

                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                if (callback != null) callback.onSuccess();
                            })
                            .addOnFailureListener(e -> {
                                if (callback != null) callback.onFailure(e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e.getMessage());
                });
    }
}
