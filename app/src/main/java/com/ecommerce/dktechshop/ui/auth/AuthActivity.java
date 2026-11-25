package com.ecommerce.dktechshop.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
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
import com.ecommerce.dktechshop.utils.NetworkMonitor;
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

        NetworkMonitor.bindOfflineBanner(this, this, findViewById(R.id.banner_no_internet), null);

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
            if (!NetworkMonitor.isConnected(this)) {
                Toast.makeText(this, NetworkMonitor.NO_INTERNET_MESSAGE, Toast.LENGTH_SHORT).show();
                return;
            }
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
                        String msg = (e.getStatusCode() == 12501)
                                ? "Đã hủy đăng nhập Google"
                                : "Đăng nhập Google thất bại. Vui lòng thử lại.";
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                    }
                }
        );

        findViewById(R.id.btn_google_sign_in).setOnClickListener(v -> {
            if (!NetworkMonitor.isConnected(this)) {
                Toast.makeText(this, NetworkMonitor.NO_INTERNET_MESSAGE, Toast.LENGTH_SHORT).show();
                return;
            }
            googleLauncher.launch(googleSignInClient.getSignInIntent());
        });
    }

    private void showForgotPasswordDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_forgot_password, null);
        TextInputLayout tilForgotEmail = dialogView.findViewById(R.id.til_forgot_email);
        TextInputEditText etForgotEmail = dialogView.findViewById(R.id.et_forgot_email);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_forgot_cancel);
        MaterialButton btnSend = dialogView.findViewById(R.id.btn_forgot_send);

        String currentEmail = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        if (!currentEmail.isEmpty()) {
            etForgotEmail.setText(currentEmail);
            etForgotEmail.setSelection(currentEmail.length());
        }

        etForgotEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int count, int after) {
                if (tilForgotEmail.getError() != null) {
                    tilForgotEmail.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSend.setOnClickListener(v -> {
            if (!NetworkMonitor.isConnected(this)) {
                Toast.makeText(this, NetworkMonitor.NO_INTERNET_MESSAGE, Toast.LENGTH_SHORT).show();
                return;
            }
            String resetEmail = etForgotEmail.getText() != null ? etForgotEmail.getText().toString().trim() : "";
            if (resetEmail.isEmpty()) {
                tilForgotEmail.setError("Vui lòng nhập email của bạn");
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(resetEmail).matches()) {
                tilForgotEmail.setError("Định dạng email không hợp lệ");
                return;
            }
            authViewModel.sendPasswordReset(resetEmail);
            dialog.dismiss();
        });

        dialog.show();
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
