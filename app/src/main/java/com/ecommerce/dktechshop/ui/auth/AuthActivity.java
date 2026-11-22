package com.ecommerce.dktechshop.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ecommerce.dktechshop.MainActivity;
import com.ecommerce.dktechshop.R;
import com.ecommerce.dktechshop.data.repository.MockRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

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
