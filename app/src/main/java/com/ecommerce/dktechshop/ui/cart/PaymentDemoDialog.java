package com.ecommerce.dktechshop.ui.cart;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.ecommerce.dktechshop.R;
import com.ecommerce.dktechshop.utils.CurrencyUtils;
import com.ecommerce.dktechshop.utils.NetworkMonitor;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Locale;

/**
 * Hộp thoại thanh toán demo tách biệt 2 trải nghiệm:
 * - VNPay / Chuyển khoản QR: hiển thị mã VietQR động + STK + đếm ngược.
 * - Ví MoMo: thanh toán trực tiếp qua ví liên kết + xác thực mã PIN 6 chữ số
 * (demo).
 */
public final class PaymentDemoDialog {

    private static final String DEMO_BANK_CODE = "SACOMBANK";
    private static final String DEMO_ACCOUNT_NO = "070389887301";
    private static final String DEMO_ACCOUNT_NAME = "DKTECHSHOP";
    private static final long PAYMENT_TIMEOUT_MS = 5 * 60 * 1000L; // 5 phút
    private static final long VERIFY_DELAY_MS = 1200L;

    private PaymentDemoDialog() {
    }

    public static void show(@NonNull Context context,
            @NonNull String paymentMethod,
            @NonNull String orderId,
            long amount,
            @Nullable String userPhone,
            @NonNull Runnable onPaymentConfirmed) {

        BottomSheetDialog dialog = new BottomSheetDialog(context, R.style.Theme_DKTechShop_BottomSheetDialog);
        View sheetView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_payment_demo, null);
        dialog.setContentView(sheetView);
        dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);

        boolean isMomo = paymentMethod.toLowerCase(Locale.ROOT).contains("momo");

        TextView tvTitle = sheetView.findViewById(R.id.tv_payment_title);
        TextView tvSubtitle = sheetView.findViewById(R.id.tv_payment_subtitle);
        TextView tvCountdown = sheetView.findViewById(R.id.tv_payment_countdown);
        TextView tvAmount = sheetView.findViewById(R.id.tv_payment_amount);
        LinearLayout layoutQrSection = sheetView.findViewById(R.id.layout_qr_section);
        LinearLayout layoutMomoSection = sheetView.findViewById(R.id.layout_momo_section);
        MaterialButton btnClose = sheetView.findViewById(R.id.btn_payment_close);
        MaterialButton btnCancel = sheetView.findViewById(R.id.btn_payment_cancel);
        MaterialButton btnConfirm = sheetView.findViewById(R.id.btn_payment_confirm);

        // MoMo views
        TextView tvMomoWalletPhone = sheetView.findViewById(R.id.tv_momo_wallet_phone);
        TextView tvMomoOrderId = sheetView.findViewById(R.id.tv_momo_order_id);
        TextInputLayout tilMomoPin = sheetView.findViewById(R.id.til_momo_pin);
        TextInputEditText etMomoPin = sheetView.findViewById(R.id.et_momo_pin);

        tvAmount.setText(CurrencyUtils.formatVND(amount));

        if (isMomo) {
            layoutQrSection.setVisibility(View.GONE);
            layoutMomoSection.setVisibility(View.VISIBLE);

            tvTitle.setText("Xác nhận Ví MoMo (Demo)");
            tvSubtitle.setText("Thanh toán nhanh qua ví MoMo đã liên kết");
            tvMomoWalletPhone.setText("Ví MoMo • " + maskPhone(userPhone));
            tvMomoOrderId.setText(orderId);
            btnConfirm.setText("Thanh toán MoMo");

            etMomoPin.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int count, int after) {
                    if (tilMomoPin.getError() != null) {
                        tilMomoPin.setError(null);
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        } else {
            layoutQrSection.setVisibility(View.VISIBLE);
            layoutMomoSection.setVisibility(View.GONE);

            tvTitle.setText("Thanh toán VNPay / QR (Demo)");
            tvSubtitle.setText("Quét mã VietQR bằng app ngân hàng bất kỳ");
            btnConfirm.setText("Tôi đã thanh toán");

            ImageView ivQr = sheetView.findViewById(R.id.iv_payment_qr);
            ProgressBar progressQr = sheetView.findViewById(R.id.progress_payment_qr);
            TextView tvBankValue = sheetView.findViewById(R.id.tv_payment_bank_value);
            TextView tvAccountValue = sheetView.findViewById(R.id.tv_payment_account_value);
            TextView tvContentValue = sheetView.findViewById(R.id.tv_payment_content_value);
            ImageButton btnCopyAccount = sheetView.findViewById(R.id.btn_copy_account);
            ImageButton btnCopyContent = sheetView.findViewById(R.id.btn_copy_content);

            tvBankValue.setText(DEMO_BANK_CODE);
            tvAccountValue.setText(DEMO_ACCOUNT_NO + " • " + DEMO_ACCOUNT_NAME);
            tvContentValue.setText(orderId);

            String qrUrl = "https://img.vietqr.io/image/" + DEMO_BANK_CODE + "-" + DEMO_ACCOUNT_NO + "-qr_only.png"
                    + "?amount=" + amount
                    + "&addInfo=" + Uri.encode(orderId)
                    + "&accountName=" + Uri.encode(DEMO_ACCOUNT_NAME);

            Glide.with(context)
                    .load(qrUrl)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target,
                                boolean isFirstResource) {
                            progressQr.setVisibility(View.GONE);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target,
                                DataSource dataSource, boolean isFirstResource) {
                            progressQr.setVisibility(View.GONE);
                            return false;
                        }
                    })
                    .error(R.drawable.ic_credit_card)
                    .into(ivQr);

            btnCopyAccount.setOnClickListener(v -> copyText(context, "Số tài khoản", DEMO_ACCOUNT_NO));
            btnCopyContent.setOnClickListener(v -> copyText(context, "Nội dung chuyển khoản", orderId));
        }

        final boolean[] isVerifying = { false };
        final Runnable[] pendingConfirm = { null };

        CountDownTimer timer = new CountDownTimer(PAYMENT_TIMEOUT_MS, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                long totalSeconds = millisUntilFinished / 1000L;
                long mins = totalSeconds / 60L;
                long secs = totalSeconds % 60L;
                tvCountdown.setText(String.format(Locale.getDefault(), "%02d:%02d", mins, secs));
            }

            @Override
            public void onFinish() {
                if (!isVerifying[0] && dialog.isShowing()) {
                    Toast.makeText(context, "Phiên thanh toán đã hết hạn, vui lòng thử lại", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }
            }
        };
        timer.start();

        View.OnClickListener closeAction = v -> {
            if (!isVerifying[0]) {
                dialog.dismiss();
            }
        };
        btnClose.setOnClickListener(closeAction);
        btnCancel.setOnClickListener(closeAction);

        btnConfirm.setOnClickListener(v -> {
            if (isVerifying[0])
                return;
            if (!NetworkMonitor.isConnected(context)) {
                Toast.makeText(context, NetworkMonitor.NO_INTERNET_MESSAGE, Toast.LENGTH_SHORT).show();
                return;
            }

            if (isMomo) {
                String pin = etMomoPin.getText() != null ? etMomoPin.getText().toString().trim() : "";
                if (pin.length() < 6) {
                    tilMomoPin.setError("Vui lòng nhập đủ 6 chữ số mã PIN");
                    etMomoPin.requestFocus();
                    return;
                }
                etMomoPin.setEnabled(false);
                hideKeyboard(context, etMomoPin);
            }

            isVerifying[0] = true;
            timer.cancel();
            dialog.setCancelable(false);
            btnConfirm.setEnabled(false);
            btnCancel.setEnabled(false);
            btnClose.setEnabled(false);
            btnConfirm.setText(isMomo ? "Đang thanh toán MoMo..." : "Đang xác thực...");

            pendingConfirm[0] = () -> {
                if (dialog.isShowing()) {
                    dialog.dismiss();
                }
                onPaymentConfirmed.run();
            };
            sheetView.postDelayed(pendingConfirm[0], VERIFY_DELAY_MS);
        });

        dialog.setOnDismissListener(d -> {
            timer.cancel();
            if (pendingConfirm[0] != null) {
                sheetView.removeCallbacks(pendingConfirm[0]);
            }
        });

        dialog.show();
    }

    private static String maskPhone(@Nullable String phone) {
        String digits = phone != null ? phone.replaceAll("\\s+", "") : "";
        if (digits.length() >= 7) {
            return digits.substring(0, 4) + " ••• " + digits.substring(digits.length() - 3);
        }
        return "0909 ••• 999";
    }

    private static void hideKeyboard(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private static void copyText(Context context, String label, String value) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText(label, value));
            Toast.makeText(context, "Đã sao chép " + label + ": " + value, Toast.LENGTH_SHORT).show();
        }
    }
}
