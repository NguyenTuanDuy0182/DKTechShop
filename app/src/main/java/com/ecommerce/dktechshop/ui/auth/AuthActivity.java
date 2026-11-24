package com.ecommerce.dktechshop.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.ecommerce.dktechshop.MainActivity;
import com.ecommerce.dktechshop.R;
import com.ecommerce.dktechshop.data.model.User;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

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
    private TextView tvForgotPassword;
    private ProgressBar progressBar;
    private MaterialButton btnSubmit;
    private TextView tvSwitchAuth;

    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Kiểm tra phiên đăng nhập từ trước, nếu đã đăng nhập thì vào thẳng MainActivity
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            navigateToMain();
            return;
        }

        setContentView(R.layout.activity_auth);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        tvTitle = findViewById(R.id.tv_auth_title);
        tvSubtitle = findViewById(R.id.tv_auth_subtitle);
        tilName = findViewById(R.id.til_name);
        tilConfirmPassword = findViewById(R.id.til_confirm_password);
        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);
        progressBar = findViewById(R.id.progress_auth);
        btnSubmit = findViewById(R.id.btn_submit_auth);
        tvSwitchAuth = findViewById(R.id.tv_switch_auth);

        tvSwitchAuth.setOnClickListener(v -> toggleAuthMode());
        tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());

        // Lắng nghe kết quả từ ViewModel
        observeViewModel();

        btnSubmit.setOnClickListener(v -> {
            String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ email và mật khẩu", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isLogin) {
                authViewModel.login(email, password);
            } else {
                String name = etName.getText() != null ? etName.getText().toString().trim() : "";
                String confirmPass = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString().trim() : "";
                if (name.isEmpty()) {
                    Toast.makeText(this, "Vui lòng nhập họ và tên", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (password.length() < 6) {
                    Toast.makeText(this, "Mật khẩu phải có ít nhất 6 ký tự", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!password.equals(confirmPass)) {
                    Toast.makeText(this, "Mật khẩu xác nhận chưa khớp", Toast.LENGTH_SHORT).show();
                    return;
                }
                authViewModel.register(email, password, name);
            }
        });

        // Đăng nhập Google
        setupGoogleSignIn();
    }

    private void observeViewModel() {
        authViewModel.getAuthSuccess().observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                User user = authViewModel.getCurrentUser().getValue();
                String name = (user != null && user.getName() != null && !user.getName().isEmpty())
                        ? user.getName() : "bạn";
                Toast.makeText(this, "Xin chào " + name + "!", Toast.LENGTH_SHORT).show();
                navigateToMain();
            }
        });

        authViewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });

        authViewModel.getSuccessMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            }
        });

        authViewModel.getIsLoading().observe(this, loading -> {
            if (progressBar != null) {
                progressBar.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
            }
            btnSubmit.setEnabled(!Boolean.TRUE.equals(loading));
        });
    }

    private void setupGoogleSignIn() {
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
                            authViewModel.loginWithGoogle(account.getIdToken());
                        }
                    } catch (ApiException e) {
                        Toast.makeText(this, "Đăng nhập Google thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
        );

        findViewById(R.id.btn_google_sign_in).setOnClickListener(v -> {
            googleLauncher.launch(googleSignInClient.getSignInIntent());
        });
    }

    private void showForgotPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Quên mật khẩu");
        builder.setMessage("Nhập email tài khoản của bạn để nhận liên kết đặt lại mật khẩu:");

        final EditText input = new EditText(this);
        input.setHint("Nhập email của bạn");
        String currentEmail = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        if (!currentEmail.isEmpty()) {
            input.setText(currentEmail);
        }
        builder.setView(input);

        builder.setPositiveButton("Gửi", (dialog, which) -> {
            String resetEmail = input.getText().toString().trim();
            if (resetEmail.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập email", Toast.LENGTH_SHORT).show();
                return;
            }
            authViewModel.sendPasswordReset(resetEmail);
        });

        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void toggleAuthMode() {
        isLogin = !isLogin;
        if (isLogin) {
            tvTitle.setText("Đăng Nhập");
            tvSubtitle.setText("Chào mừng bạn quay trở lại ứng dụng");
            tilName.setVisibility(View.GONE);
            tilConfirmPassword.setVisibility(View.GONE);
            if (tvForgotPassword != null) tvForgotPassword.setVisibility(View.VISIBLE);
            btnSubmit.setText("Đăng nhập");
            tvSwitchAuth.setText("Chưa có tài khoản? Đăng ký ngay");
        } else {
            tvTitle.setText("Đăng Ký");
            tvSubtitle.setText("Tạo tài khoản TechGear để mua sắm");
            tilName.setVisibility(View.VISIBLE);
            tilConfirmPassword.setVisibility(View.VISIBLE);
            if (tvForgotPassword != null) tvForgotPassword.setVisibility(View.GONE);
            btnSubmit.setText("Đăng ký tài khoản");
            tvSwitchAuth.setText("Đã có tài khoản? Đăng nhập ngay");
        }
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
