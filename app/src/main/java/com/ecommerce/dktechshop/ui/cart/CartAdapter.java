package com.ecommerce.dktechshop.ui.cart;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.ecommerce.dktechshop.R;
import com.ecommerce.dktechshop.data.model.CartItem;
import com.ecommerce.dktechshop.utils.CurrencyUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.ViewHolder> {

    public interface OnCartItemActionListener {
        void onQuantityChanged(CartItem item, int delta);
        void onSelectionChanged(CartItem item, boolean isSelected);
    }

    private List<CartItem> cartItems = new ArrayList<>();
    private Set<String> selectedIds = new HashSet<>();
    private final OnCartItemActionListener listener;

    public CartAdapter(OnCartItemActionListener listener) {
        this.listener = listener;
    }

    public void setCartItems(List<CartItem> items) {
        this.cartItems = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setSelectedIds(Set<String> ids) {
        this.selectedIds = ids != null ? new HashSet<>(ids) : new HashSet<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CartItem item = cartItems.get(position);
        holder.tvName.setText(item.getProductName());
        holder.tvVariant.setText(item.getVariant());
        holder.tvPrice.setText(CurrencyUtils.formatVND(item.getPrice()));
        holder.tvQty.setText(String.valueOf(item.getQuantity()));

        boolean isSelected = selectedIds.contains(item.getItemId());
        holder.cbSelect.setOnCheckedChangeListener(null);
        holder.cbSelect.setChecked(isSelected);
        holder.cbSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) {
                listener.onSelectionChanged(item, isChecked);
            }
        });

        int strokeColor = ContextCompat.getColor(
                holder.itemView.getContext(),
                isSelected ? R.color.md_theme_light_primary : R.color.md_theme_light_outlineVariant
        );
        holder.cardItem.setStrokeColor(strokeColor);

        holder.itemView.setOnClickListener(v -> holder.cbSelect.performClick());

        int fallbackIcon;
        if ("mouse".equalsIgnoreCase(item.getCategory())) {
            fallbackIcon = R.drawable.ic_mouse;
        } else if ("headphone".equalsIgnoreCase(item.getCategory())) {
            fallbackIcon = R.drawable.ic_headset;
        } else {
            fallbackIcon = R.drawable.ic_devices;
        }

        if (item.getThumbnailUrl() != null && !item.getThumbnailUrl().trim().isEmpty()) {
            holder.ivIcon.setImageTintList(null);
            Glide.with(holder.itemView.getContext())
                    .load(item.getThumbnailUrl())
                    .placeholder(fallbackIcon)
                    .error(fallbackIcon)
                    .centerCrop()
                    .into(holder.ivIcon);
        } else {
            holder.ivIcon.setImageTintList(ContextCompat.getColorStateList(holder.itemView.getContext(), R.color.md_theme_light_primary));
            holder.ivIcon.setImageResource(fallbackIcon);
        }

        holder.btnMinus.setOnClickListener(v -> {
            if (listener != null) listener.onQuantityChanged(item, -1);
        });

        holder.btnPlus.setOnClickListener(v -> {
            if (listener != null) listener.onQuantityChanged(item, 1);
        });
    }

    @Override
    public int getItemCount() {
        return cartItems != null ? cartItems.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardItem;
        MaterialCheckBox cbSelect;
        ImageView ivIcon;
        TextView tvName;
        TextView tvVariant;
        TextView tvPrice;
        TextView tvQty;
        MaterialButton btnMinus;
        MaterialButton btnPlus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardItem = itemView.findViewById(R.id.card_cart_item);
            cbSelect = itemView.findViewById(R.id.cb_cart_item);
            ivIcon = itemView.findViewById(R.id.iv_cart_icon);
            tvName = itemView.findViewById(R.id.tv_cart_name);
            tvVariant = itemView.findViewById(R.id.tv_cart_variant);
            tvPrice = itemView.findViewById(R.id.tv_cart_price);
            tvQty = itemView.findViewById(R.id.tv_cart_qty);
            btnMinus = itemView.findViewById(R.id.btn_cart_minus);
            btnPlus = itemView.findViewById(R.id.btn_cart_plus);
        }
    }
}
