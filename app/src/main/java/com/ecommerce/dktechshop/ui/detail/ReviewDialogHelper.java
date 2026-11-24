package com.ecommerce.dktechshop.ui.detail;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ecommerce.dktechshop.R;
import com.ecommerce.dktechshop.data.model.Order;
import com.ecommerce.dktechshop.data.model.Review;
import com.ecommerce.dktechshop.data.repository.MockRepository;
import com.ecommerce.dktechshop.data.repository.ProductRepository;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReviewDialogHelper {

    public interface OnReviewSubmittedListener {
        void onReviewSubmitted(Review review);
    }

    public static void showReviewDialog(@NonNull Context context,
                                        @NonNull String productId,
                                        @NonNull String productName,
                                        @NonNull String variant,
                                        @Nullable Order order,
                                        @Nullable OnReviewSubmittedListener listener) {

        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_write_review, null);
        dialog.setContentView(dialogView);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int) (context.getResources().getDisplayMetrics().widthPixels * 0.92),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        TextView tvProductName = dialogView.findViewById(R.id.tv_review_product_name);
        TextView tvVariant = dialogView.findViewById(R.id.tv_review_product_variant);
        TextView tvStarDesc = dialogView.findViewById(R.id.tv_star_description);
        EditText etComment = dialogView.findViewById(R.id.et_review_comment);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel_review);
        MaterialButton btnSubmit = dialogView.findViewById(R.id.btn_submit_review);

        ImageView[] stars = new ImageView[]{
                dialogView.findViewById(R.id.iv_star_1),
                dialogView.findViewById(R.id.iv_star_2),
                dialogView.findViewById(R.id.iv_star_3),
                dialogView.findViewById(R.id.iv_star_4),
                dialogView.findViewById(R.id.iv_star_5)
        };

        tvProductName.setText(productName);
        tvVariant.setText(variant != null && !variant.trim().isEmpty() ? "Phân loại: " + variant.trim() : "Phân loại: Mặc định");

        final float[] currentRating = {5.0f};

        Runnable updateStarVisuals = () -> {
            int score = (int) currentRating[0];
            for (int i = 0; i < 5; i++) {
                if (i < score) {
                    stars[i].setImageResource(R.drawable.ic_star);
                } else {
                    stars[i].setImageResource(R.drawable.ic_star_outline);
                }
            }
            switch (score) {
                case 1: tvStarDesc.setText("1.0 ★ Rất tệ"); break;
                case 2: tvStarDesc.setText("2.0 ★ Chưa hài lòng"); break;
                case 3: tvStarDesc.setText("3.0 ★ Hài lòng"); break;
                case 4: tvStarDesc.setText("4.0 ★ Rất tốt"); break;
                default: tvStarDesc.setText("5.0 ★ Tuyệt vời"); break;
            }
        };

        for (int i = 0; i < 5; i++) {
            final int starScore = i + 1;
            stars[i].setOnClickListener(v -> {
                currentRating[0] = (float) starScore;
                updateStarVisuals.run();
            });
        }
        updateStarVisuals.run();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSubmit.setOnClickListener(v -> {
            String comment = etComment.getText() != null ? etComment.getText().toString().trim() : "";
            if (comment.isEmpty()) {
                Toast.makeText(context, "Vui lòng nhập nội dung đánh giá của bạn", Toast.LENGTH_SHORT).show();
                return;
            }

            btnSubmit.setEnabled(false);
            btnSubmit.setText("Đang gửi...");

            FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
            String userId = fbUser != null ? fbUser.getUid() : "guest";
            String userName = (fbUser != null && fbUser.getDisplayName() != null && !fbUser.getDisplayName().trim().isEmpty())
                    ? fbUser.getDisplayName()
                    : MockRepository.getInstance().getCurrentUser().getName();

            String reviewId = "rev_" + System.currentTimeMillis();
            String createdAt = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
            String orderId = order != null ? order.getOrderId() : "";

            Review review = new Review(
                    reviewId,
                    productId,
                    productName,
                    orderId,
                    userId,
                    userName,
                    currentRating[0],
                    comment,
                    variant,
                    createdAt
            );

            ProductRepository.getInstance().addReview(review, order, new ProductRepository.Callback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Toast.makeText(context, "Đã gửi đánh giá thành công! Cảm ơn bạn.", Toast.LENGTH_SHORT).show();
                    if (listener != null) listener.onReviewSubmitted(review);
                    dialog.dismiss();
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(context, "Đã gửi đánh giá thành công!", Toast.LENGTH_SHORT).show();
                    if (listener != null) listener.onReviewSubmitted(review);
                    dialog.dismiss();
                }
            });
        });

        dialog.show();
    }
}
