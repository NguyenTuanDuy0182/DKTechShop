package com.ecommerce.dktechshop.data.repository;

import com.ecommerce.dktechshop.data.model.Address;
import com.ecommerce.dktechshop.data.model.User;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserInfo;
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
    private User cachedUser;
    private String activeSignInProvider;

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
     * Kiểm tra phiên đăng nhập hiện tại có phải đăng nhập bằng Email/Mật khẩu hay không.
     * Trả về false nếu phiên hiện tại đăng nhập bằng Google (kể cả khi tài khoản từng liên kết password trước đó).
     */
    public boolean isPasswordSignInUser() {
        FirebaseUser fbUser = mAuth.getCurrentUser();
        if (fbUser == null) return false;

        if (GoogleAuthProvider.PROVIDER_ID.equals(activeSignInProvider)) {
            return false;
        }
        if (EmailAuthProvider.PROVIDER_ID.equals(activeSignInProvider)) {
            return true;
        }

        boolean hasPassword = false;
        boolean hasGoogle = false;
        for (UserInfo info : fbUser.getProviderData()) {
            if (info == null) continue;
            String pid = info.getProviderId();
            if (EmailAuthProvider.PROVIDER_ID.equals(pid)) {
                hasPassword = true;
            } else if (GoogleAuthProvider.PROVIDER_ID.equals(pid)) {
                hasGoogle = true;
            }
        }
        // Nếu có provider google.com mà chưa đọc xong JWT sign_in_provider -> ưu tiên khóa an toàn cho đến khi token xác nhận
        return hasPassword && !hasGoogle;
    }

    /**
     * Đọc chính xác sign_in_provider của phiên hiện tại từ Firebase ID Token (JWT claim).
     */
    public void resolveSignInProvider(Runnable onResolved) {
        FirebaseUser fbUser = mAuth.getCurrentUser();
        if (fbUser == null) {
            activeSignInProvider = null;
            if (onResolved != null) onResolved.run();
            return;
        }
        fbUser.getIdToken(false)
                .addOnSuccessListener(result -> {
                    if (result != null && result.getSignInProvider() != null) {
                        activeSignInProvider = result.getSignInProvider();
                    }
                    if (onResolved != null) onResolved.run();
                })
                .addOnFailureListener(e -> {
                    if (onResolved != null) onResolved.run();
                });
    }

    public User getCachedUser() {
        return cachedUser;
    }

    public void setCachedUser(User user) {
        this.cachedUser = user;
    }

    /**
     * Đăng ký tài khoản bằng email/mật khẩu và tự động lưu document vào users/{uid} trên Firestore.
     */
    public void signUp(String email, String password, String name, AuthCallback callback) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                        activeSignInProvider = EmailAuthProvider.PROVIDER_ID;
                        FirebaseUser fbUser = mAuth.getCurrentUser();
                        String uid = fbUser.getUid();

                        // Cập nhật displayName vào Firebase Auth profile
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setDisplayName(name)
                                .build();
                        fbUser.updateProfile(profileUpdates);

                        // Tạo hồ sơ người dùng lưu trên Firestore
                        User newUser = new User(uid, name, email, "", "", new ArrayList<>(), true);

                        mFirestore.collection("users").document(uid).set(newUser)
                                .addOnSuccessListener(aVoid -> {
                                    cachedUser = newUser;
                                    callback.onSuccess(newUser);
                                })
                                .addOnFailureListener(e -> callback.onFailure("Lỗi lưu hồ sơ: " + e.getMessage()));
                    } else {
                        callback.onFailure(mapAuthError(task.getException(), "Đăng ký thất bại. Vui lòng thử lại."));
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
                        activeSignInProvider = EmailAuthProvider.PROVIDER_ID;
                        fetchCurrentUser(callback);
                    } else {
                        callback.onFailure(mapAuthError(task.getException(), "Đăng nhập thất bại. Vui lòng thử lại."));
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
                        activeSignInProvider = GoogleAuthProvider.PROVIDER_ID;
                        fetchCurrentUser(callback);
                    } else {
                        callback.onFailure(mapAuthError(task.getException(), "Đăng nhập Google thất bại. Vui lòng thử lại."));
                    }
                });
    }

    /**
     * Lấy dữ liệu người dùng bất kỳ theo uid từ Firestore users/{uid}.
     */
    public void fetchUserById(String uid, AuthCallback callback) {
        if (uid == null || uid.trim().isEmpty()) {
            if (callback != null) callback.onFailure("Mã người dùng không hợp lệ");
            return;
        }

        mFirestore.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        User user = doc.toObject(User.class);
                        if (user != null) {
                            if (user.getUid() == null || user.getUid().isEmpty()) {
                                user.setUid(doc.getId());
                            }
                            if (callback != null) callback.onSuccess(user);
                            return;
                        }
                    }
                    if (callback != null) callback.onFailure("Không tìm thấy người dùng");
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e.getMessage());
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
                        mFirestore.collection("users").document(uid).set(user);
                    }
                    user.setLoggedIn(true);
                    cachedUser = user;
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
        if (!isPasswordSignInUser()) {
            callback.onFailure("Tài khoản đăng nhập bằng Google không hỗ trợ đổi mật khẩu.");
            return;
        }

        fbUser.updatePassword(newPassword)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(mapAuthError(e, "Không thể đổi mật khẩu. Vui lòng thử lại.")));
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
                .addOnSuccessListener(aVoid -> {
                    if (cachedUser != null) {
                        cachedUser.setName(newName);
                        cachedUser.setPhone(newPhone);
                    }
                    callback.onSuccess();
                })
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
                .addOnFailureListener(e -> callback.onFailure(mapAuthError(e, "Không thể gửi email khôi phục mật khẩu.")));
    }

    /**
     * Chuyển đổi các ngoại lệ Firebase Auth sang thông báo tiếng Việt thân thiện.
     */
    private String mapAuthError(Exception e, String fallback) {
        if (e == null) return fallback;
        if (e instanceof FirebaseNetworkException) {
            return "Không có kết nối mạng. Vui lòng kiểm tra Wi-Fi hoặc dữ liệu di động.";
        }
        if (e instanceof FirebaseTooManyRequestsException) {
            return "Thao tác quá nhiều lần. Vui lòng thử lại sau ít phút.";
        }
        if (e instanceof FirebaseAuthWeakPasswordException) {
            return "Mật khẩu quá yếu. Vui lòng nhập ít nhất 6 ký tự.";
        }
        if (e instanceof FirebaseAuthUserCollisionException) {
            return "Email này đã được đăng ký tài khoản.";
        }
        if (e instanceof FirebaseAuthInvalidUserException) {
            return "Tài khoản không tồn tại hoặc đã bị vô hiệu hóa.";
        }
        if (e instanceof FirebaseAuthInvalidCredentialsException) {
            String code = ((FirebaseAuthInvalidCredentialsException) e).getErrorCode();
            if ("ERROR_INVALID_EMAIL".equals(code)) {
                return "Định dạng email không hợp lệ.";
            }
            return "Email hoặc mật khẩu không chính xác.";
        }
        if (e instanceof FirebaseAuthRecentLoginRequiredException) {
            return "Phiên đăng nhập đã cũ. Vui lòng đăng xuất và đăng nhập lại để tiếp tục.";
        }
        return fallback;
    }

    /**
     * Đăng xuất.
     */
    public void signOut() {
        mAuth.signOut();
        cachedUser = null;
        activeSignInProvider = null;
    }
}
