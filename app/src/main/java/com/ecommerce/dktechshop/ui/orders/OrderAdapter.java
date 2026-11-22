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
import com.ecommerce.dktechshop.data.repository.MockRepository;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.ViewHolder> {

    public interface OnOrderActionListener {
        void onActionClick(Order order);
    }

    private List<Order> orderList = new ArrayList<>();
    private final OnOrderActionListener listener;

    public OrderAdapter(OnOrderActionListener listener) {
        this.listener = listener;
    }

    public void setOrders(List<Order> orders) {
        this.orderList = orders;
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
        holder.tvOrderId.setText(o.getOrderId());
        holder.tvDatePayment.setText(o.getCreatedAt() + " • " + o.getPaymentMethod());
        holder.tvStatusPill.setText(o.getStatus());
        holder.tvTotal.setText(MockRepository.formatVND(o.getTotalAmount()));

        // Status pill style
        if ("Đang xử lý".equalsIgnoreCase(o.getStatus())) {
            holder.tvStatusPill.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.md_theme_light_tertiaryContainer)));
            holder.tvStatusPill.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.md_theme_light_onTertiaryContainer));
            holder.btnAction.setText("Cập nhật trạng thái →");
            holder.btnAction.setVisibility(View.VISIBLE);
        } else if ("Đang giao".equalsIgnoreCase(o.getStatus())) {
            holder.tvStatusPill.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.md_theme_light_primaryContainer)));
            holder.tvStatusPill.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.md_theme_light_onPrimaryContainer));
            holder.btnAction.setText("Cập nhật trạng thái →");
            holder.btnAction.setVisibility(View.VISIBLE);
        } else {
            // Hoàn thành
            holder.tvStatusPill.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.green_container)));
            holder.tvStatusPill.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.green_accent));
            holder.btnAction.setText("Mua lại");
            holder.btnAction.setVisibility(View.VISIBLE);
        }

        // Populate items sub-container
        holder.layoutItemsContainer.removeAllViews();
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
            tvItemPrice.setText(MockRepository.formatVND(item.getPrice() * item.getQuantity()));
            tvItemPrice.setTextSize(12);
            tvItemPrice.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.md_theme_light_onSurface));
            row.addView(tvItemPrice);

            holder.layoutItemsContainer.addView(row);
        }

        holder.btnAction.setOnClickListener(v -> {
            if (listener != null) listener.onActionClick(o);
        });
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
