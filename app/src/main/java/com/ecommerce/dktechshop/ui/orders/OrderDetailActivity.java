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

import com.ecommerce.dktechshop.MainActivity;
import com.ecommerce.dktechshop.R;
import com.ecommerce.dktechshop.data.model.CartItem;
import com.ecommerce.dktechshop.data.model.Order;
import com.ecommerce.dktechshop.data.model.OrderItem;
import com.ecommerce.dktechshop.data.model.Product;
import com.ecommerce.dktechshop.data.repository.CartRepository;
import com.ecommerce.dktechshop.data.repository.MockRepository;
import com.ecommerce.dktechshop.data.repository.OrderRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.ListenerRegistration;

public class OrderDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ORDER = "EXTRA_ORDER";
    public static final String EXTRA_ORDER_ID = "EXTRA_ORDER_ID";

    private Order currentOrder;
    private ListenerRegistration orderListenerRegistration;

    private TextView tvToolbarOrderId;
    private TextView tvTopStatusPill;
    private MaterialCardView cardStatusBanner;
    private ImageView ivStatusIcon;
    private TextView tvStatusTitle;
    private TextView tvStatusDesc;
    private TextView tvOrderIdValue;
    private TextView tvOrderCreatedAt;
    private TextView tvRecipientName;
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
            orderListenerRegistration = OrderRepository.getInstance().listenSingleOrder(orderId, new OrderRepository.OrderCallback() {
                @Override
                public void onSuccess(Order order) {
                    currentOrder = order;
                    bindOrder(order);
                }

                @Override
                public void onFailure(String error) {
                    if (currentOrder == null) {
                        Toast.makeText(OrderDetailActivity.this, "Lỗi tải đơn hàng: " + error, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void initViews() {
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

        String address = (order.getShippingAddress() != null && !order.getShippingAddress().trim().isEmpty())
                ? order.getShippingAddress()
                : "227 Nguyễn Văn Cừ, Phường 4, Quận 5, TP. Hồ Chí Minh";
        tvShippingAddress.setText(address);

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
        tvSubtotal.setText(MockRepository.formatVND(order.getTotalAmount()));
        tvTotalAmount.setText(MockRepository.formatVND(order.getTotalAmount()));
    }

    private View createOrderItemView(OrderItem item) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 6, 0, 6);

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
        tvQtyPrice.setText(item.getQuantity() + " × " + MockRepository.formatVND(item.getPrice()));
        tvQtyPrice.setTextSize(11);
        tvQtyPrice.setTextColor(ContextCompat.getColor(this, R.color.md_theme_light_onSurfaceVariant));
        tvQtyPrice.setPadding(0, 2, 0, 0);
        textCol.addView(tvQtyPrice);

        row.addView(textCol);

        TextView tvItemTotal = new TextView(this);
        tvItemTotal.setText(MockRepository.formatVND(item.getPrice() * item.getQuantity()));
        tvItemTotal.setTextSize(13);
        tvItemTotal.setTextColor(ContextCompat.getColor(this, R.color.md_theme_light_onSurface));
        tvItemTotal.setTypeface(null, android.graphics.Typeface.BOLD);
        row.addView(tvItemTotal);

        return row;
    }

    private void handleCancelOrder() {
        if (currentOrder == null) return;
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
                            currentOrder.setStatus("Đã hủy");
                            bindOrder(currentOrder);
                            Toast.makeText(OrderDetailActivity.this, "Đã hủy đơn hàng thành công!", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void handleConfirmReceived() {
        if (currentOrder == null) return;
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
                            currentOrder.setStatus("Đã giao");
                            bindOrder(currentOrder);
                            Toast.makeText(OrderDetailActivity.this, "Cảm ơn bạn! Đơn hàng đã hoàn thành.", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Chưa nhận", null)
                .show();
    }

    private void handleReorder() {
        if (currentOrder == null || currentOrder.getItems() == null || currentOrder.getItems().isEmpty()) {
            Toast.makeText(this, "Không có sản phẩm để mua lại", Toast.LENGTH_SHORT).show();
            return;
        }

        for (OrderItem oi : currentOrder.getItems()) {
            Product p = MockRepository.getInstance().getProductById(oi.getProductId());
            String thumb = (p != null) ? p.getThumbnailUrl() : "";
            String category = (p != null) ? p.getCategory() : "";
            String itemId = CartRepository.generateItemId(oi.getProductId(), oi.getVariant());
            CartItem ci = new CartItem(
                    itemId,
                    oi.getProductId(),
                    oi.getProductName(),
                    category,
                    oi.getPrice(),
                    oi.getVariant(),
                    oi.getQuantity(),
                    thumb
            );
            CartRepository.getInstance().upsertCartItem(ci, null);
            if (p != null) {
                MockRepository.getInstance().addToCart(p, oi.getVariant(), oi.getQuantity());
            }
        }

        Toast.makeText(this, "Đã thêm các món vào giỏ hàng!", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("navigate_tab", R.id.nav_cart);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
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
