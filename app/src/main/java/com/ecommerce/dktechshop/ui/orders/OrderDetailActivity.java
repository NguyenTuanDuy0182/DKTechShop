package com.ecommerce.dktechshop.ui.orders;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.ecommerce.dktechshop.MainActivity;
import com.ecommerce.dktechshop.R;
import com.ecommerce.dktechshop.data.model.Order;
import com.ecommerce.dktechshop.data.model.OrderItem;
import com.ecommerce.dktechshop.data.model.User;
import com.ecommerce.dktechshop.data.repository.AuthRepository;
import com.ecommerce.dktechshop.data.repository.CartRepository;
import com.ecommerce.dktechshop.data.repository.OrderRepository;
import com.ecommerce.dktechshop.ui.detail.ReviewDialogHelper;
import com.ecommerce.dktechshop.utils.CurrencyUtils;
import com.ecommerce.dktechshop.utils.NetworkMonitor;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;

public class OrderDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ORDER = "EXTRA_ORDER";
    public static final String EXTRA_ORDER_ID = "EXTRA_ORDER_ID";

    private Order currentOrder;
    private ListenerRegistration orderListenerRegistration;
    private SwipeRefreshLayout swipeRefresh;

    private TextView tvToolbarOrderId;
    private TextView tvTopStatusPill;
    private MaterialCardView cardStatusBanner;
    private ImageView ivStatusIcon;
    private TextView tvStatusTitle;
    private TextView tvStatusDesc;
    private TextView tvOrderIdValue;
    private TextView tvOrderCreatedAt;
    private TextView tvRecipientName;
    private TextView tvRecipientPhone;
    private TextView tvShippingAddress;
    private TextView tvPaymentMethod;
    private TextView tvPaymentDesc;
    private TextView tvItemsCount;
    private LinearLayout layoutOrderItemsList;
    private TextView tvSubtotal;
    private TextView tvTotalAmount;
    private MaterialButton btnCancelOrder;
    private MaterialButton btnConfirmReceived;
    private MaterialButton btnReorder;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        initViews();

        if (getIntent().hasExtra(EXTRA_ORDER)) {
            currentOrder = (Order) getIntent().getSerializableExtra(EXTRA_ORDER);
        }

        String orderId = getIntent().getStringExtra(EXTRA_ORDER_ID);
        if (orderId == null && currentOrder != null) {
            orderId = currentOrder.getOrderId();
        }

        if (currentOrder != null) {
            bindOrder(currentOrder);
        }

        // Lắng nghe cập nhật đơn hàng theo thời gian thực từ Firestore
        if (orderId != null && !orderId.trim().isEmpty()) {
            final String finalOrderId = orderId;
            Runnable reloadOrder = () -> {
                if (orderListenerRegistration != null) {
                    orderListenerRegistration.remove();
                }
                orderListenerRegistration = OrderRepository.getInstance().listenSingleOrder(finalOrderId, new OrderRepository.OrderCallback() {
                    @Override
                    public void onSuccess(Order order) {
                        if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                        currentOrder = order;
                        bindOrder(order);
                    }

                    @Override
                    public void onFailure(String error) {
                        if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                        if (currentOrder == null) {
                            Toast.makeText(OrderDetailActivity.this, "Lỗi tải đơn hàng: " + error, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            };

            NetworkMonitor.bindOfflineBanner(this, this, findViewById(R.id.banner_no_internet), reloadOrder);

            if (swipeRefresh != null) {
                swipeRefresh.setColorSchemeResources(R.color.md_theme_light_primary);
                swipeRefresh.setOnRefreshListener(() -> {
                    if (!NetworkMonitor.isConnected(this)) {
                        swipeRefresh.setRefreshing(false);
                        Toast.makeText(this, NetworkMonitor.NO_INTERNET_MESSAGE, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    reloadOrder.run();
                    swipeRefresh.postDelayed(() -> {
                        if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                    }, 1000);
                });
            }

            reloadOrder.run();
        } else {
            NetworkMonitor.bindOfflineBanner(this, this, findViewById(R.id.banner_no_internet), null);
        }
    }

    private void initViews() {
        swipeRefresh = findViewById(R.id.swipe_refresh_order_detail);
        ImageButton btnBack = findViewById(R.id.btn_back);
        tvToolbarOrderId = findViewById(R.id.tv_toolbar_order_id);
        tvTopStatusPill = findViewById(R.id.tv_top_status_pill);
        cardStatusBanner = findViewById(R.id.card_status_banner);
        ivStatusIcon = findViewById(R.id.iv_status_icon);
        tvStatusTitle = findViewById(R.id.tv_status_title);
        tvStatusDesc = findViewById(R.id.tv_status_desc);
        tvOrderIdValue = findViewById(R.id.tv_order_id_value);
        ImageButton btnCopyOrderId = findViewById(R.id.btn_copy_order_id);
        tvOrderCreatedAt = findViewById(R.id.tv_order_created_at);
        tvRecipientName = findViewById(R.id.tv_recipient_name);
        tvRecipientPhone = findViewById(R.id.tv_recipient_phone);
        tvShippingAddress = findViewById(R.id.tv_shipping_address);
        tvPaymentMethod = findViewById(R.id.tv_payment_method);
        tvPaymentDesc = findViewById(R.id.tv_payment_desc);
        tvItemsCount = findViewById(R.id.tv_items_count);
        layoutOrderItemsList = findViewById(R.id.layout_order_items_list);
        tvSubtotal = findViewById(R.id.tv_subtotal);
        tvTotalAmount = findViewById(R.id.tv_total_amount);
        btnCancelOrder = findViewById(R.id.btn_cancel_order);
        btnConfirmReceived = findViewById(R.id.btn_confirm_received);
        btnReorder = findViewById(R.id.btn_reorder);

        btnBack.setOnClickListener(v -> finish());

        btnCopyOrderId.setOnClickListener(v -> {
            if (currentOrder != null && currentOrder.getOrderId() != null) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Order ID", currentOrder.getOrderId());
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(this, "Đã sao chép mã đơn hàng: " + currentOrder.getOrderId(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnCancelOrder.setOnClickListener(v -> handleCancelOrder());
        btnConfirmReceived.setOnClickListener(v -> handleConfirmReceived());
        btnReorder.setOnClickListener(v -> handleReorder());
    }

    private void bindOrder(Order order) {
        if (order == null) return;

        tvToolbarOrderId.setText("#" + order.getOrderId());
        tvOrderIdValue.setText("#" + order.getOrderId());
        tvOrderCreatedAt.setText(order.getCreatedAt());

        // Hiển thị tên người nhận
        String recipientName = order.getRecipientName();
        if (recipientName == null || recipientName.trim().isEmpty()) {
            FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
            User u = AuthRepository.getInstance().getCachedUser();
            if (fbUser != null && fbUser.getDisplayName() != null && !fbUser.getDisplayName().trim().isEmpty()) {
                recipientName = fbUser.getDisplayName();
            } else if (u != null && u.getName() != null && !u.getName().trim().isEmpty()) {
                recipientName = u.getName();
            } else {
                recipientName = "Khách hàng";
            }
        }
        tvRecipientName.setText("Người nhận: " + recipientName);

        // Hiển thị số điện thoại người nhận
        String recipientPhone = order.getRecipientPhone();
        if (recipientPhone == null || recipientPhone.trim().isEmpty()) {
            User u = AuthRepository.getInstance().getCachedUser();
            if (u != null && u.getPhone() != null && !u.getPhone().trim().isEmpty()) {
                recipientPhone = u.getPhone();
            }
        }
        if (recipientPhone != null && !recipientPhone.trim().isEmpty()) {
            tvRecipientPhone.setVisibility(View.VISIBLE);
            tvRecipientPhone.setText("Số điện thoại: " + recipientPhone.trim());
        } else {
            tvRecipientPhone.setVisibility(View.GONE);
        }

        // Tự động đồng bộ tên và SĐT nếu đơn hàng cũ trên Firestore chưa lưu trực tiếp
        String orderUserId = order.getUserId();
        if ((order.getRecipientName() == null || order.getRecipientName().trim().isEmpty()
                || order.getRecipientPhone() == null || order.getRecipientPhone().trim().isEmpty())) {
            if (orderUserId == null || orderUserId.trim().isEmpty()) {
                FirebaseUser cur = FirebaseAuth.getInstance().getCurrentUser();
                if (cur != null) orderUserId = cur.getUid();
            }
            if (orderUserId != null && !orderUserId.trim().isEmpty()) {
                AuthRepository.getInstance().fetchUserById(orderUserId, new AuthRepository.AuthCallback() {
                    @Override
                    public void onSuccess(User dbUser) {
                        if (dbUser != null) {
                            if ((order.getRecipientName() == null || order.getRecipientName().trim().isEmpty())
                                    && dbUser.getName() != null && !dbUser.getName().trim().isEmpty()) {
                                tvRecipientName.setText("Người nhận: " + dbUser.getName());
                            }
                            if ((order.getRecipientPhone() == null || order.getRecipientPhone().trim().isEmpty())
                                    && dbUser.getPhone() != null && !dbUser.getPhone().trim().isEmpty()) {
                                tvRecipientPhone.setVisibility(View.VISIBLE);
                                tvRecipientPhone.setText("Số điện thoại: " + dbUser.getPhone().trim());
                            }
                        }
                    }

                    @Override
                    public void onFailure(String error) {}
                });
            }
        }

        // Hiển thị địa chỉ giao nhận
        String address = (order.getShippingAddress() != null && !order.getShippingAddress().trim().isEmpty())
                ? order.getShippingAddress().trim()
                : "Chưa cập nhật địa chỉ";
        if (!address.toLowerCase().startsWith("địa chỉ:")) {
            tvShippingAddress.setText("Địa chỉ: " + address);
        } else {
            tvShippingAddress.setText(address);
        }

        String payment = (order.getPaymentMethod() != null && !order.getPaymentMethod().trim().isEmpty())
                ? order.getPaymentMethod()
                : "COD";
        tvPaymentMethod.setText(payment);
        if ("COD".equalsIgnoreCase(payment)) {
            tvPaymentDesc.setText("Thanh toán bằng tiền mặt khi nhận hàng tận nơi.");
        } else if (payment.toLowerCase().contains("vnpay")) {
            tvPaymentDesc.setText("Đã hoàn tất thanh toán qua cổng điện tử VNPay QR.");
        } else {
            tvPaymentDesc.setText("Phương thức thanh toán đã được xác nhận.");
        }

        // Cập nhật giao diện trạng thái trực quan bằng màu sắc: Đang xử lý, Đang giao, Đã giao, Đã hủy
        String status = order.getStatus() != null ? order.getStatus().trim() : "Đang xử lý";

        if ("Đang xử lý".equalsIgnoreCase(status)) {
            tvTopStatusPill.setText("● Đang xử lý");
            tvTopStatusPill.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.status_processing_bg)));
            tvTopStatusPill.setTextColor(ContextCompat.getColor(this, R.color.status_processing_text));

            cardStatusBanner.setCardBackgroundColor(ContextCompat.getColor(this, R.color.status_processing_bg));
            ivStatusIcon.setImageResource(R.drawable.ic_schedule);
            ivStatusIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.status_processing_text)));
            tvStatusTitle.setText("Đơn hàng đang được xử lý");
            tvStatusTitle.setTextColor(ContextCompat.getColor(this, R.color.status_processing_text));
            tvStatusDesc.setText("Đơn hàng của bạn đã được ghi nhận. Cửa hàng đang chuẩn bị và đóng gói sản phẩm.");
            tvStatusDesc.setTextColor(ContextCompat.getColor(this, R.color.status_processing_text));

            btnCancelOrder.setVisibility(View.VISIBLE);
            btnConfirmReceived.setVisibility(View.GONE);
            btnReorder.setVisibility(View.GONE);

        } else if ("Đang giao".equalsIgnoreCase(status)) {
            tvTopStatusPill.setText("● Đang giao");
            tvTopStatusPill.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.status_delivering_bg)));
            tvTopStatusPill.setTextColor(ContextCompat.getColor(this, R.color.status_delivering_text));

            cardStatusBanner.setCardBackgroundColor(ContextCompat.getColor(this, R.color.status_delivering_bg));
            ivStatusIcon.setImageResource(R.drawable.ic_local_shipping);
            ivStatusIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.status_delivering_text)));
            tvStatusTitle.setText("Đơn hàng đang được giao");
            tvStatusTitle.setTextColor(ContextCompat.getColor(this, R.color.status_delivering_text));
            tvStatusDesc.setText("Kiện hàng đang được nhân viên vận chuyển giao đến bạn. Vui lòng giữ liên lạc điện thoại.");
            tvStatusDesc.setTextColor(ContextCompat.getColor(this, R.color.status_delivering_text));

            btnCancelOrder.setVisibility(View.GONE);
            btnConfirmReceived.setVisibility(View.VISIBLE);
            btnReorder.setVisibility(View.GONE);

        } else if ("Đã hủy".equalsIgnoreCase(status)) {
            tvTopStatusPill.setText("● Đã hủy");
            tvTopStatusPill.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.status_cancelled_bg)));
            tvTopStatusPill.setTextColor(ContextCompat.getColor(this, R.color.status_cancelled_text));

            cardStatusBanner.setCardBackgroundColor(ContextCompat.getColor(this, R.color.status_cancelled_bg));
            ivStatusIcon.setImageResource(R.drawable.ic_close);
            ivStatusIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.status_cancelled_text)));
            tvStatusTitle.setText("Đơn hàng đã bị hủy");
            tvStatusTitle.setTextColor(ContextCompat.getColor(this, R.color.status_cancelled_text));
            tvStatusDesc.setText("Đơn hàng này đã bị hủy. Bạn có thể nhấn 'Mua lại các sản phẩm' nếu muốn đặt lại món.");
            tvStatusDesc.setTextColor(ContextCompat.getColor(this, R.color.status_cancelled_text));

            btnCancelOrder.setVisibility(View.GONE);
            btnConfirmReceived.setVisibility(View.GONE);
            btnReorder.setVisibility(View.VISIBLE);

        } else {
            // "Đã giao" hoặc "Hoàn thành"
            tvTopStatusPill.setText("● " + (status.equalsIgnoreCase("Hoàn thành") ? "Đã giao" : status));
            tvTopStatusPill.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.status_delivered_bg)));
            tvTopStatusPill.setTextColor(ContextCompat.getColor(this, R.color.status_delivered_text));

            cardStatusBanner.setCardBackgroundColor(ContextCompat.getColor(this, R.color.status_delivered_bg));
            ivStatusIcon.setImageResource(R.drawable.ic_check);
            ivStatusIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.status_delivered_text)));
            tvStatusTitle.setText("Giao hàng thành công");
            tvStatusTitle.setTextColor(ContextCompat.getColor(this, R.color.status_delivered_text));
            tvStatusDesc.setText("Đơn hàng đã được giao thành công. Cảm ơn bạn đã tin tưởng mua sắm tại DKTechShop!");
            tvStatusDesc.setTextColor(ContextCompat.getColor(this, R.color.status_delivered_text));

            btnCancelOrder.setVisibility(View.GONE);
            btnConfirmReceived.setVisibility(View.GONE);
            btnReorder.setVisibility(View.VISIBLE);
        }

        // Render danh sách từng sản phẩm đã mua kèm giá tiền
        layoutOrderItemsList.removeAllViews();
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            tvItemsCount.setText(order.getItems().size() + " món");
            for (int i = 0; i < order.getItems().size(); i++) {
                OrderItem item = order.getItems().get(i);
                View itemView = createOrderItemView(item);
                layoutOrderItemsList.addView(itemView);

                if (i < order.getItems().size() - 1) {
                    View divider = new View(this);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
                    lp.setMargins(0, 8, 0, 8);
                    divider.setLayoutParams(lp);
                    divider.setBackgroundColor(ContextCompat.getColor(this, R.color.md_theme_light_surfaceContainerHigh));
                    layoutOrderItemsList.addView(divider);
                }
            }
        } else {
            tvItemsCount.setText("0 món");
        }

        // Cập nhật thông tin chi tiết thanh toán
        tvSubtotal.setText(CurrencyUtils.formatVND(order.getTotalAmount()));
        tvTotalAmount.setText(CurrencyUtils.formatVND(order.getTotalAmount()));
    }

    private View createOrderItemView(OrderItem item) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(0, 6, 0, 6);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lpText = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textCol.setLayoutParams(lpText);

        TextView tvName = new TextView(this);
        tvName.setText(item.getProductName());
        tvName.setTextSize(13);
        tvName.setTextColor(ContextCompat.getColor(this, R.color.md_theme_light_onSurface));
        tvName.setTypeface(null, android.graphics.Typeface.BOLD);
        textCol.addView(tvName);

        TextView tvVariant = new TextView(this);
        tvVariant.setText("Phân loại: " + item.getVariant());
        tvVariant.setTextSize(11);
        tvVariant.setTextColor(ContextCompat.getColor(this, R.color.md_theme_light_onSurfaceVariant));
        tvVariant.setPadding(0, 2, 0, 0);
        textCol.addView(tvVariant);

        TextView tvQtyPrice = new TextView(this);
        tvQtyPrice.setText(item.getQuantity() + " × " + CurrencyUtils.formatVND(item.getPrice()));
        tvQtyPrice.setTextSize(11);
        tvQtyPrice.setTextColor(ContextCompat.getColor(this, R.color.md_theme_light_onSurfaceVariant));
        tvQtyPrice.setPadding(0, 2, 0, 0);
        textCol.addView(tvQtyPrice);

        row.addView(textCol);

        TextView tvItemTotal = new TextView(this);
        tvItemTotal.setText(CurrencyUtils.formatVND(item.getPrice() * item.getQuantity()));
        tvItemTotal.setTextSize(13);
        tvItemTotal.setTextColor(ContextCompat.getColor(this, R.color.md_theme_light_onSurface));
        tvItemTotal.setTypeface(null, android.graphics.Typeface.BOLD);
        row.addView(tvItemTotal);

        container.addView(row);

        // Hiển thị nút "★ Đánh giá sản phẩm" nếu đơn hàng đã giao thành công
        String status = (currentOrder != null && currentOrder.getStatus() != null) ? currentOrder.getStatus().trim() : "";
        if ("Đã giao".equalsIgnoreCase(status) || "Hoàn thành".equalsIgnoreCase(status)) {
            LinearLayout actionRow = new LinearLayout(this);
            actionRow.setOrientation(LinearLayout.HORIZONTAL);
            actionRow.setGravity(android.view.Gravity.END);
            actionRow.setPadding(0, 8, 0, 0);

            MaterialButton btnReview = new MaterialButton(this);
            int h = (int) (36 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams lpBtn = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, h);
            btnReview.setLayoutParams(lpBtn);
            btnReview.setTextSize(11);
            btnReview.setCornerRadius((int) (18 * getResources().getDisplayMetrics().density));
            btnReview.setPadding(24, 0, 24, 0);

            if (item.isReviewed()) {
                btnReview.setText("✓ Đã đánh giá");
                btnReview.setEnabled(false);
                btnReview.setTextColor(ContextCompat.getColor(this, R.color.md_theme_light_onSurfaceVariant));
                btnReview.setBackgroundColor(ContextCompat.getColor(this, R.color.md_theme_light_surfaceContainerHigh));
            } else {
                btnReview.setText("★ Đánh giá sản phẩm");
                btnReview.setEnabled(true);
                btnReview.setTextColor(ContextCompat.getColor(this, R.color.md_theme_light_primary));
                btnReview.setStrokeColorResource(R.color.md_theme_light_primary);
                btnReview.setStrokeWidth(1);
                btnReview.setBackgroundColor(ContextCompat.getColor(this, R.color.md_theme_light_primaryContainer));

                btnReview.setOnClickListener(v -> {
                    if (!NetworkMonitor.isConnected(this)) {
                        Toast.makeText(this, NetworkMonitor.NO_INTERNET_MESSAGE, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (item.isReviewed()) {
                        Toast.makeText(this, "Bạn đã đánh giá sản phẩm này cho đơn hàng này rồi", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String pId = item.getProductId();
                    if (pId == null || pId.trim().isEmpty()) {
                        pId = "prod_custom";
                    }

                    ReviewDialogHelper.showReviewDialog(this, pId, item.getProductName(), item.getVariant(), currentOrder, review -> {
                        item.setReviewed(true);
                        btnReview.setText("✓ Đã đánh giá");
                        btnReview.setEnabled(false);
                        btnReview.setTextColor(ContextCompat.getColor(this, R.color.md_theme_light_onSurfaceVariant));
                        btnReview.setBackgroundColor(ContextCompat.getColor(this, R.color.md_theme_light_surfaceContainerHigh));
                        btnReview.setStrokeWidth(0);
                    });
                });
            }

            actionRow.addView(btnReview);
            container.addView(actionRow);
        }

        return container;
    }

    private void handleCancelOrder() {
        if (currentOrder == null) return;
        if (!NetworkMonitor.isConnected(this)) {
            Toast.makeText(this, NetworkMonitor.NO_INTERNET_MESSAGE, Toast.LENGTH_SHORT).show();
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle("Hủy đơn hàng")
                .setMessage("Bạn có chắc chắn muốn hủy đơn hàng #" + currentOrder.getOrderId() + " không?")
                .setPositiveButton("Hủy đơn", (dialog, which) -> {
                    OrderRepository.getInstance().updateOrderStatus(currentOrder.getOrderId(), "Đã hủy", new OrderRepository.ActionCallback() {
                        @Override
                        public void onSuccess() {
                            currentOrder.setStatus("Đã hủy");
                            bindOrder(currentOrder);
                            Toast.makeText(OrderDetailActivity.this, "Đã hủy đơn hàng thành công!", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(String error) {
                            Toast.makeText(OrderDetailActivity.this, "Không thể hủy đơn: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void handleConfirmReceived() {
        if (currentOrder == null) return;
        if (!NetworkMonitor.isConnected(this)) {
            Toast.makeText(this, NetworkMonitor.NO_INTERNET_MESSAGE, Toast.LENGTH_SHORT).show();
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle("Xác nhận đã nhận hàng")
                .setMessage("Bạn xác nhận đã nhận đầy đủ kiện hàng cho đơn #" + currentOrder.getOrderId() + "?")
                .setPositiveButton("Đã nhận hàng", (dialog, which) -> {
                    OrderRepository.getInstance().updateOrderStatus(currentOrder.getOrderId(), "Đã giao", new OrderRepository.ActionCallback() {
                        @Override
                        public void onSuccess() {
                            currentOrder.setStatus("Đã giao");
                            bindOrder(currentOrder);
                            Toast.makeText(OrderDetailActivity.this, "Cảm ơn bạn! Đơn hàng đã hoàn thành.", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(String error) {
                            Toast.makeText(OrderDetailActivity.this, "Không thể cập nhật: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Chưa nhận", null)
                .show();
    }

    private void handleReorder() {
        if (!NetworkMonitor.isConnected(this)) {
            Toast.makeText(this, NetworkMonitor.NO_INTERNET_MESSAGE, Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentOrder == null || currentOrder.getItems() == null || currentOrder.getItems().isEmpty()) {
            Toast.makeText(this, "Không có sản phẩm để mua lại", Toast.LENGTH_SHORT).show();
            return;
        }

        new androidx.lifecycle.ViewModelProvider(this).get(com.ecommerce.dktechshop.ui.cart.CartViewModel.class)
                .reorder(currentOrder, new CartRepository.ActionCallback() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(OrderDetailActivity.this, "Đã thêm các món vào giỏ hàng!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(OrderDetailActivity.this, MainActivity.class);
                        intent.putExtra("navigate_tab", R.id.nav_cart);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onFailure(String error) {
                        Toast.makeText(OrderDetailActivity.this, error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (orderListenerRegistration != null) {
            orderListenerRegistration.remove();
            orderListenerRegistration = null;
        }
    }
}
