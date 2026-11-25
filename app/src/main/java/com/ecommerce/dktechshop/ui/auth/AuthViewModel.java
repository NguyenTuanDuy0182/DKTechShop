package com.ecommerce.dktechshop.ui.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.ecommerce.dktechshop.data.model.Address;
import com.ecommerce.dktechshop.data.model.User;
import com.ecommerce.dktechshop.data.repository.AuthRepository;

import java.util.ArrayList;
import java.util.List;

public class AuthViewModel extends ViewModel {

    private final AuthRepository authRepository;

    private final MutableLiveData<User> currentUser = new MutableLiveData<>();
    private final MutableLiveData<List<Address>> addresses = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> authSuccess = new MutableLiveData<>(false);

    public AuthViewModel() {
        this.authRepository = AuthRepository.getInstance();
        loadCurrentUser();
    }

    public LiveData<User> getCurrentUser() {
        return currentUser;
    }

    public LiveData<List<Address>> getAddresses() {
        return addresses;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<String> getSuccessMessage() {
        return successMessage;
    }

    public LiveData<Boolean> getAuthSuccess() {
        return authSuccess;
    }

    public boolean isPasswordSignInUser() {
        return authRepository.isPasswordSignInUser();
    }

    public void clearStatus() {
        errorMessage.setValue(null);
        successMessage.setValue(null);
        authSuccess.setValue(false);
    }

    /**
     * Tải thông tin người dùng từ Firestore nếu đã có phiên đăng nhập.
     */
    public void loadCurrentUser() {
        if (authRepository.getCurrentFirebaseUser() != null) {
            authRepository.fetchCurrentUser(new AuthRepository.AuthCallback() {
                @Override
                public void onSuccess(User user) {
                    currentUser.setValue(user);
                    loadAddresses();
                }

                @Override
                public void onFailure(String error) {
                    // Không cần show lỗi nếu là kiểm tra phiên nền
                }
            });
        }
    }

    /**
     * Đăng nhập email/password.
     */
    public void login(String email, String password) {
        isLoading.setValue(true);
        clearStatus();
        authRepository.signIn(email, password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                isLoading.setValue(false);
                currentUser.setValue(user);
                loadAddresses();
                authSuccess.setValue(true);
            }

            @Override
            public void onFailure(String error) {
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }

    /**
     * Đăng ký tài khoản mới và tạo hồ sơ Firestore.
     */
    public void register(String email, String password, String name) {
        isLoading.setValue(true);
        clearStatus();
        authRepository.signUp(email, password, name, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                isLoading.setValue(false);
                currentUser.setValue(user);
                loadAddresses();
                authSuccess.setValue(true);
            }

            @Override
            public void onFailure(String error) {
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }

    /**
     * Đăng nhập bằng Google ID Token.
     */
    public void loginWithGoogle(String idToken) {
        isLoading.setValue(true);
        clearStatus();
        authRepository.signInWithGoogle(idToken, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                isLoading.setValue(false);
                currentUser.setValue(user);
                loadAddresses();
                authSuccess.setValue(true);
            }

            @Override
            public void onFailure(String error) {
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }

    /**
     * Cập nhật thông tin cá nhân (họ tên, số điện thoại).
     */
    public void updateDisplayName(String name, String phone) {
        isLoading.setValue(true);
        clearStatus();
        authRepository.updateUserName(name, phone, new AuthRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                isLoading.setValue(false);
                User user = currentUser.getValue();
                if (user != null) {
                    user.setName(name);
                    user.setPhone(phone);
                    currentUser.setValue(user);
                }
                successMessage.setValue("Đã cập nhật thông tin thành công!");
            }

            @Override
            public void onFailure(String error) {
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }

    /**
     * Đổi mật khẩu tài khoản.
     */
    public void changePassword(String newPassword) {
        isLoading.setValue(true);
        clearStatus();
        authRepository.updatePassword(newPassword, new AuthRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                isLoading.setValue(false);
                successMessage.setValue("Đã đổi mật khẩu thành công!");
            }

            @Override
            public void onFailure(String error) {
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }

    /**
     * Thêm địa chỉ mới vào subcollection users/{uid}/addresses.
     */
    public void addAddress(String label, String detail) {
        User user = currentUser.getValue();
        String phone = user != null ? user.getPhone() : "";
        addAddress(label, detail, phone);
    }

    public void addAddress(String label, String detail, String phone) {
        isLoading.setValue(true);
        clearStatus();
        boolean isDefault = (addresses.getValue() == null || addresses.getValue().isEmpty());
        Address newAddress = new Address("", label, detail, phone, isDefault);

        authRepository.addAddress(newAddress, new AuthRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                isLoading.setValue(false);
                successMessage.setValue("Đã thêm địa chỉ mới thành công!");
                loadAddresses();
            }

            @Override
            public void onFailure(String error) {
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }

    public void deleteAddress(String addressId) {
        isLoading.setValue(true);
        clearStatus();
        authRepository.deleteAddress(addressId, new AuthRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                isLoading.setValue(false);
                successMessage.setValue("Đã xóa địa chỉ!");
                loadAddresses();
            }

            @Override
            public void onFailure(String error) {
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }

    public void updateAddress(Address address) {
        isLoading.setValue(true);
        clearStatus();
        authRepository.updateAddress(address, new AuthRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                isLoading.setValue(false);
                successMessage.setValue("Đã cập nhật địa chỉ thành công!");
                loadAddresses();
            }

            @Override
            public void onFailure(String error) {
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }

    /**
     * Đặt địa chỉ mặc định.
     */
    public void setDefaultAddress(String addressId) {
        isLoading.setValue(true);
        authRepository.setDefaultAddress(addressId, new AuthRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                isLoading.setValue(false);
                successMessage.setValue("Đã đổi địa chỉ mặc định!");
                loadAddresses();
            }

            @Override
            public void onFailure(String error) {
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }

    /**
     * Lấy danh sách địa chỉ từ Firestore.
     */
    public void loadAddresses() {
        authRepository.getAddresses(new AuthRepository.AddressListCallback() {
            @Override
            public void onSuccess(List<Address> list) {
                addresses.setValue(list);
                User user = currentUser.getValue();
                if (user != null) {
                    user.setAddresses(list);
                    authRepository.setCachedUser(user);
                }
            }

            @Override
            public void onFailure(String error) {
                // ignore
            }
        });
    }

    /**
     * Gửi email khôi phục mật khẩu.
     */
    public void sendPasswordReset(String email) {
        isLoading.setValue(true);
        clearStatus();
        authRepository.sendPasswordResetEmail(email, new AuthRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                isLoading.setValue(false);
                successMessage.setValue("Đã gửi email khôi phục mật khẩu. Vui lòng kiểm tra hộp thư!");
            }

            @Override
            public void onFailure(String error) {
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }

    /**
     * Đăng xuất khỏi Firebase.
     */
    public void logout() {
        authRepository.signOut();
        currentUser.setValue(null);
        addresses.setValue(new ArrayList<>());
    }
}
