package com.ecommerce.dktechshop.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.ecommerce.dktechshop.MainActivity;
import com.ecommerce.dktechshop.R;
import com.ecommerce.dktechshop.data.model.User;
import com.ecommerce.dktechshop.data.repository.MockRepository;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class AuthActivity extends AppCompatActivity {

    private boolean isLogin = true;
    private TextView tvTitle;
    private TextView tvSubtitle;
    private TextInputLayout tilName;
    private TextInputLayout tilConfirmPassword;
    private TextInputEditText etName;
    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private TextInputEditText etConfirmPassword;
    private MaterialButton btnSubmit;
    private TextView tvSwitchAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        tvTitle = findViewById(R.id.tv_auth_title);
        tvSubtitle = findViewById(R.id.tv_auth_subtitle);
        tilName = findViewById(R.id.til_name);
        tilConfirmPassword = findViewById(R.id.til_confirm_password);
        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        btnSubmit = findViewById(R.id.btn_submit_auth);
        tvSwitchAuth = findViewById(R.id.tv_switch_auth);

        tvSwitchAuth.setOnClickListener(v -> toggleAuthMode());

        btnSubmit.setOnClickListener(v -> {
            String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isLogin) {
                String name = etName.getText() != null ? etName.getText().toString().trim() : "";
                String confirmPass = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString().trim() : "";
                if (!password.equals(confirmPass)) {
                    Toast.makeText(this, "Mật khẩu xác nhận chưa khớp", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!name.isEmpty()) {
                    MockRepository.getInstance().getCurrentUser().setName(name);
                }
            }

            MockRepository.getInstance().getCurrentUser().setLoggedIn(true);
            Toast.makeText(this, "Xin chào " + MockRepository.getInstance().getCurrentUser().getName() + "!", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        // Đăng nhập Google
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, gso);

        ActivityResultLauncher<Intent> googleLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        if (account != null && account.getIdToken() != null) {
                            firebaseAuthWithGoogle(account.getIdToken());
                        }
                    } catch (ApiException e) {
                        Toast.makeText(this, "Đăng nhập Google thất bại", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        findViewById(R.id.btn_google_sign_in).setOnClickListener(v -> {
            googleLauncher.launch(googleSignInClient.getSignInIntent());
        });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
                        User currentUser = MockRepository.getInstance().getCurrentUser();
                        if (fbUser != null) {
                            currentUser.setUid(fbUser.getUid());
                            if (fbUser.getDisplayName() != null) {
                                currentUser.setName(fbUser.getDisplayName());
                            }
                            if (fbUser.getEmail() != null) {
                                currentUser.setEmail(fbUser.getEmail());
                            }
                        }
                        currentUser.setLoggedIn(true);
                        Toast.makeText(this, "Xin chào " + currentUser.getName() + "!", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(this, "Lỗi đăng nhập Firebase", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void toggleAuthMode() {
        isLogin = !isLogin;
        if (isLogin) {
            tvTitle.setText("Đăng Nhập");
            tvSubtitle.setText("Chào mừng bạn quay trở lại ứng dụng");
            tilName.setVisibility(View.GONE);
            tilConfirmPassword.setVisibility(View.GONE);
            btnSubmit.setText("Đăng nhập");
            tvSwitchAuth.setText("Chưa có tài khoản? Đăng ký ngay");
        } else {
            tvTitle.setText("Đăng Ký");
            tvSubtitle.setText("Tạo tài khoản TechGear để mua sắm");
            tilName.setVisibility(View.VISIBLE);
            tilConfirmPassword.setVisibility(View.VISIBLE);
            btnSubmit.setText("Đăng ký tài khoản");
            tvSwitchAuth.setText("Đã có tài khoản? Đăng nhập ngay");
        }
    }
}
