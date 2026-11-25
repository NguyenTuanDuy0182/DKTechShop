package com.ecommerce.dktechshop.ui.orders;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.ecommerce.dktechshop.R;
import com.ecommerce.dktechshop.data.model.Order;
import com.ecommerce.dktechshop.data.model.OrderItem;
import com.ecommerce.dktechshop.utils.CurrencyUtils;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.ViewHolder> {

    public interface OnOrderActionListener {
        void onCancelOrder(Order order);
        void onConfirmReceived(Order order);
        void onReorder(Order order);
        void onOrderClick(Order order);
    }

    private List<Order> orderList = new ArrayList<>();
    private final OnOrderActionListener listener;

    public OrderAdapter(OnOrderActionListener listener) {
        this.listener = listener;
    }

    public void setOrders(List<Order> orders) {
        this.orderList = orders != null ? orders : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Order o = orderList.get(position);
        holder.tvOrderId.setText("#" + o.getOrderId());
        holder.tvDatePayment.setText(o.getCreatedAt() + " • " + o.getPaymentMethod());
        holder.tvTotal.setText(CurrencyUtils.formatVND(o.getTotalAmount()));

        // Item click listener để mở OrderDetailActivity
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onOrderClick(o);
        });

        // Thiết lập trạng thái đơn hàng trực quan bằng màu sắc: Đang xử lý, Đang giao, Đã giao, Đã hủy
        String status = o.getStatus() != null ? o.getStatus().trim() : "Đang xử lý";

        if ("Đang xử lý".equalsIgnoreCase(status)) {
            holder.tvStatusPill.setText("● Đang xử lý");
            holder.tvStatusPill.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.status_processing_bg)));
            holder.tvStatusPill.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.status_processing_text));

            holder.btnAction.setText("Hủy đơn");
            holder.btnAction.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.status_cancelled_bg)));
            holder.btnAction.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.status_cancelled_text));
            holder.btnAction.setVisibility(View.VISIBLE);
            holder.btnAction.setOnClickListener(v -> {
                if (listener != null) listener.onCancelOrder(o);
            });
        } else if ("Đang giao".equalsIgnoreCase(status)) {
            holder.tvStatusPill.setText("● Đang giao");
            holder.tvStatusPill.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.status_delivering_bg)));
            holder.tvStatusPill.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.status_delivering_text));

            holder.btnAction.setText("Đã nhận hàng");
            holder.btnAction.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.status_delivering_bg)));
            holder.btnAction.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.status_delivering_text));
            holder.btnAction.setVisibility(View.VISIBLE);
            holder.btnAction.setOnClickListener(v -> {
                if (listener != null) listener.onConfirmReceived(o);
            });
        } else if ("Đã hủy".equalsIgnoreCase(status)) {
            holder.tvStatusPill.setText("● Đã hủy");
            holder.tvStatusPill.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.status_cancelled_bg)));
            holder.tvStatusPill.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.status_cancelled_text));

            holder.btnAction.setText("Mua lại");
            holder.btnAction.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.md_theme_light_surfaceContainerHigh)));
            holder.btnAction.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.md_theme_light_onSurface));
            holder.btnAction.setVisibility(View.VISIBLE);
            holder.btnAction.setOnClickListener(v -> {
                if (listener != null) listener.onReorder(o);
            });
        } else {
            // "Đã giao" hoặc "Hoàn thành"
            holder.tvStatusPill.setText("● " + (status.equalsIgnoreCase("Hoàn thành") ? "Đã giao" : status));
            holder.tvStatusPill.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.status_delivered_bg)));
            holder.tvStatusPill.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.status_delivered_text));

            holder.btnAction.setText("Mua lại");
            holder.btnAction.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.status_delivered_bg)));
            holder.btnAction.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.status_delivered_text));
            holder.btnAction.setVisibility(View.VISIBLE);
            holder.btnAction.setOnClickListener(v -> {
                if (listener != null) listener.onReorder(o);
            });
        }

        // Hiển thị danh sách sản phẩm tóm tắt trong thẻ đơn hàng
        holder.layoutItemsContainer.removeAllViews();
        if (o.getItems() != null) {
            for (OrderItem item : o.getItems()) {
                LinearLayout row = new LinearLayout(holder.itemView.getContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(0, 4, 0, 4);

                LinearLayout textCol = new LinearLayout(holder.itemView.getContext());
                textCol.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams lpText = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                textCol.setLayoutParams(lpText);

                TextView tvItemName = new TextView(holder.itemView.getContext());
                tvItemName.setText(item.getProductName());
                tvItemName.setTextSize(12);
                tvItemName.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.md_theme_light_onSurface));
                textCol.addView(tvItemName);

                TextView tvItemVariant = new TextView(holder.itemView.getContext());
                tvItemVariant.setText(item.getVariant() + " × " + item.getQuantity());
                tvItemVariant.setTextSize(10);
                tvItemVariant.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.md_theme_light_onSurfaceVariant));
                textCol.addView(tvItemVariant);

                row.addView(textCol);

                TextView tvItemPrice = new TextView(holder.itemView.getContext());
                tvItemPrice.setText(CurrencyUtils.formatVND(item.getPrice() * item.getQuantity()));
                tvItemPrice.setTextSize(12);
                tvItemPrice.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.md_theme_light_onSurface));
                row.addView(tvItemPrice);

                holder.layoutItemsContainer.addView(row);
            }
        }
    }

    @Override
    public int getItemCount() {
        return orderList != null ? orderList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderId;
        TextView tvDatePayment;
        TextView tvStatusPill;
        LinearLayout layoutItemsContainer;
        TextView tvTotal;
        MaterialButton btnAction;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.tv_order_id);
            tvDatePayment = itemView.findViewById(R.id.tv_order_date_payment);
            tvStatusPill = itemView.findViewById(R.id.tv_order_status_pill);
            layoutItemsContainer = itemView.findViewById(R.id.layout_order_items_container);
            tvTotal = itemView.findViewById(R.id.tv_order_total);
            btnAction = itemView.findViewById(R.id.btn_order_action);
        }
    }
}
