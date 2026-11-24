package com.ecommerce.dktechshop.data.repository;

import com.ecommerce.dktechshop.data.model.Address;
import com.ecommerce.dktechshop.data.model.User;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuthRepository {

    private static AuthRepository instance;
    private final FirebaseAuth mAuth;
    private final FirebaseFirestore mFirestore;

    public interface AuthCallback {
        void onSuccess(User user);
        void onFailure(String error);
    }

    public interface ActionCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public interface AddressListCallback {
        void onSuccess(List<Address> addresses);
        void onFailure(String error);
    }

    public static synchronized AuthRepository getInstance() {
        if (instance == null) {
            instance = new AuthRepository();
        }
        return instance;
    }

    public AuthRepository() {
        this.mAuth = FirebaseAuth.getInstance();
        this.mFirestore = FirebaseFirestore.getInstance();
    }

    public FirebaseUser getCurrentFirebaseUser() {
        return mAuth.getCurrentUser();
    }

    /**
     * Đăng ký tài khoản bằng email/mật khẩu và tự động lưu document vào users/{uid} trên Firestore.
     */
    public void signUp(String email, String password, String name, AuthCallback callback) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                        FirebaseUser fbUser = mAuth.getCurrentUser();
                        String uid = fbUser.getUid();

                        // Cập nhật displayName vào Firebase Auth profile
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setDisplayName(name)
                                .build();
                        fbUser.updateProfile(profileUpdates);

                        // Tạo hồ sơ người dùng lưu trên Firestore
                        User newUser = new User(uid, name, email, "", "", new ArrayList<>(), true);
                        newUser.setTier("Thành viên Đồng");

                        mFirestore.collection("users").document(uid).set(newUser)
                                .addOnSuccessListener(aVoid -> callback.onSuccess(newUser))
                                .addOnFailureListener(e -> callback.onFailure("Lỗi lưu hồ sơ: " + e.getMessage()));
                    } else {
                        String msg = task.getException() != null ? task.getException().getMessage() : "Đăng ký thất bại";
                        callback.onFailure(msg);
                    }
                });
    }

    /**
     * Đăng nhập bằng email/mật khẩu và lấy dữ liệu hồ sơ từ Firestore.
     */
    public void signIn(String email, String password, AuthCallback callback) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                        fetchCurrentUser(callback);
                    } else {
                        String msg = task.getException() != null ? task.getException().getMessage() : "Đăng nhập thất bại";
                        callback.onFailure(msg);
                    }
                });
    }

    /**
     * Đăng nhập bằng Google ID Token và đồng bộ hồ sơ Firestore.
     */
    public void signInWithGoogle(String idToken, AuthCallback callback) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                        fetchCurrentUser(callback);
                    } else {
                        String msg = task.getException() != null ? task.getException().getMessage() : "Đăng nhập Google thất bại";
                        callback.onFailure(msg);
                    }
                });
    }

    /**
     * Lấy dữ liệu người dùng hiện tại từ Firestore users/{uid}.
     */
    public void fetchCurrentUser(AuthCallback callback) {
        FirebaseUser fbUser = mAuth.getCurrentUser();
        if (fbUser == null) {
            callback.onFailure("Người dùng chưa đăng nhập");
            return;
        }

        String uid = fbUser.getUid();
        mFirestore.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    User user;
                    if (doc.exists()) {
                        user = doc.toObject(User.class);
                        if (user == null) {
                            String name = fbUser.getDisplayName() != null ? fbUser.getDisplayName() : "Người dùng";
                            String email = fbUser.getEmail() != null ? fbUser.getEmail() : "";
                            user = new User(uid, name, email, "", "", new ArrayList<>(), true);
                        }
                    } else {
                        // Nếu chưa có doc (ví dụ đăng nhập Google lần đầu) -> tạo doc mới
                        String name = fbUser.getDisplayName() != null ? fbUser.getDisplayName() : "Người dùng";
                        String email = fbUser.getEmail() != null ? fbUser.getEmail() : "";
                        user = new User(uid, name, email, "", "", new ArrayList<>(), true);
                        user.setTier("Thành viên Đồng");
                        mFirestore.collection("users").document(uid).set(user);
                    }
                    user.setLoggedIn(true);
                    callback.onSuccess(user);
                })
                .addOnFailureListener(e -> callback.onFailure("Lỗi tải thông tin: " + e.getMessage()));
    }

    /**
     * Đổi mật khẩu tài khoản thông qua FirebaseUser.
     */
    public void updatePassword(String newPassword, ActionCallback callback) {
        FirebaseUser fbUser = mAuth.getCurrentUser();
        if (fbUser == null) {
            callback.onFailure("Chưa đăng nhập");
            return;
        }

        fbUser.updatePassword(newPassword)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Cập nhật thông tin cá nhân (tên, số điện thoại) lên Firestore users/{uid} và Firebase Auth profile.
     */
    public void updateUserName(String newName, String newPhone, ActionCallback callback) {
        FirebaseUser fbUser = mAuth.getCurrentUser();
        if (fbUser == null) {
            callback.onFailure("Chưa đăng nhập");
            return;
        }

        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(newName)
                .build();
        fbUser.updateProfile(profileUpdates);

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", newName);
        updates.put("phone", newPhone);

        mFirestore.collection("users").document(fbUser.getUid()).update(updates)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Thêm địa chỉ mới vào subcollection users/{uid}/addresses.
     */
    public void addAddress(Address address, ActionCallback callback) {
        FirebaseUser fbUser = mAuth.getCurrentUser();
        if (fbUser == null) {
            callback.onFailure("Chưa đăng nhập");
            return;
        }

        DocumentReference docRef = mFirestore.collection("users")
                .document(fbUser.getUid())
                .collection("addresses")
                .document();

        address.setId(docRef.getId());
        docRef.set(address)
                .addOnSuccessListener(aVoid -> {
                    // Nếu địa chỉ là mặc định -> cập nhật trường address ở user
                    if (address.isDefault()) {
                        mFirestore.collection("users").document(fbUser.getUid())
                                .update("address", address.getDetail());
                    }
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Đặt địa chỉ mặc định trong subcollection users/{uid}/addresses.
     */
    public void setDefaultAddress(String addressId, ActionCallback callback) {
        FirebaseUser fbUser = mAuth.getCurrentUser();
        if (fbUser == null) {
            callback.onFailure("Chưa đăng nhập");
            return;
        }

        String uid = fbUser.getUid();
        mFirestore.collection("users").document(uid).collection("addresses").get()
                .addOnSuccessListener(querySnapshot -> {
                    WriteBatch batch = mFirestore.batch();
                    String defaultDetail = "";
                    for (DocumentSnapshot doc : querySnapshot) {
                        boolean isTarget = doc.getId().equals(addressId);
                        batch.update(doc.getReference(), "default", isTarget);
                        if (isTarget) {
                            String detail = doc.getString("detail");
                            if (detail != null) defaultDetail = detail;
                        }
                    }
                    if (!defaultDetail.isEmpty()) {
                        batch.update(mFirestore.collection("users").document(uid), "address", defaultDetail);
                    }
                    batch.commit()
                            .addOnSuccessListener(aVoid -> callback.onSuccess())
                            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void deleteAddress(String addressId, ActionCallback callback) {
        FirebaseUser fbUser = mAuth.getCurrentUser();
        if (fbUser == null) {
            callback.onFailure("Chưa đăng nhập");
            return;
        }

        mFirestore.collection("users")
                .document(fbUser.getUid())
                .collection("addresses")
                .document(addressId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void updateAddress(Address address, ActionCallback callback) {
        FirebaseUser fbUser = mAuth.getCurrentUser();
        if (fbUser == null) {
            callback.onFailure("Chưa đăng nhập");
            return;
        }

        mFirestore.collection("users")
                .document(fbUser.getUid())
                .collection("addresses")
                .document(address.getId())
                .set(address)
                .addOnSuccessListener(aVoid -> {
                    if (address.isDefault()) {
                        mFirestore.collection("users").document(fbUser.getUid())
                                .update("address", address.getDetail());
                    }
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Lấy danh sách địa chỉ từ subcollection users/{uid}/addresses.
     */
    public void getAddresses(AddressListCallback callback) {
        FirebaseUser fbUser = mAuth.getCurrentUser();
        if (fbUser == null) {
            callback.onFailure("Chưa đăng nhập");
            return;
        }

        mFirestore.collection("users")
                .document(fbUser.getUid())
                .collection("addresses")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Address> list = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot) {
                        Address a = doc.toObject(Address.class);
                        if (a != null) {
                            if (a.getId() == null || a.getId().isEmpty()) {
                                a.setId(doc.getId());
                            }
                            list.add(a);
                        }
                    }
                    callback.onSuccess(list);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Gửi email khôi phục mật khẩu.
     */
    public void sendPasswordResetEmail(String email, ActionCallback callback) {
        mAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Đăng xuất.
     */
    public void signOut() {
        mAuth.signOut();
        MockRepository.getInstance().getCurrentUser().setLoggedIn(false);
    }
}
