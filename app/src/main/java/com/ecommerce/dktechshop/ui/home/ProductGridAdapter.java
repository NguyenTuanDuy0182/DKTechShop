package com.ecommerce.dktechshop.ui.home;

import android.graphics.Paint;
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
import com.ecommerce.dktechshop.data.model.Product;
import com.ecommerce.dktechshop.data.repository.MockRepository;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class ProductGridAdapter extends RecyclerView.Adapter<ProductGridAdapter.ViewHolder> {

    public interface OnProductClickListener {
        void onProductClick(Product product);
        void onQuickAddClick(Product product);
    }

    private List<Product> productList = new ArrayList<>();
    private final OnProductClickListener listener;

    public ProductGridAdapter(OnProductClickListener listener) {
        this.listener = listener;
    }

    public void setProducts(List<Product> products) {
        this.productList = products;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product_grid, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product p = productList.get(position);
        holder.tvBrand.setText(p.getBrand());
        holder.tvName.setText(p.getName());
        holder.tvPrice.setText(MockRepository.formatVND(p.getPrice()));
        if (p.getOriginalPrice() > p.getPrice() && p.getDiscountPercent() > 0) {
            holder.tvOriginalPrice.setVisibility(View.VISIBLE);
            holder.tvDiscountTag.setVisibility(View.VISIBLE);
            holder.tvOriginalPrice.setText(MockRepository.formatVND(p.getOriginalPrice()));
            holder.tvOriginalPrice.setPaintFlags(holder.tvOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvDiscountTag.setText("-" + p.getDiscountPercent() + "%");
        } else {
            holder.tvOriginalPrice.setVisibility(View.GONE);
            holder.tvDiscountTag.setVisibility(View.GONE);
        }

        if (p.getThumbnailUrl() != null && !p.getThumbnailUrl().trim().isEmpty()) {
            holder.ivIcon.setImageTintList(null);
            holder.ivIcon.clearColorFilter();
            Glide.with(holder.itemView.getContext())
                    .load(p.getThumbnailUrl())
                    .into(holder.ivIcon);
        } else {
            holder.ivIcon.setImageTintList(ContextCompat.getColorStateList(holder.itemView.getContext(), R.color.md_theme_light_primary));
            if ("mouse".equalsIgnoreCase(p.getCategory())) {
                holder.ivIcon.setImageResource(R.drawable.ic_mouse);
            } else if ("headphone".equalsIgnoreCase(p.getCategory())) {
                holder.ivIcon.setImageResource(R.drawable.ic_headset);
            } else {
                holder.ivIcon.setImageResource(R.drawable.ic_devices);
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onProductClick(p);
        });

        holder.btnQuickAdd.setOnClickListener(v -> {
            if (listener != null) listener.onQuickAddClick(p);
        });
    }

    @Override
    public int getItemCount() {
        return productList != null ? productList.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvDiscountTag;
        TextView tvBrand;
        TextView tvName;
        TextView tvOriginalPrice;
        TextView tvPrice;
        MaterialButton btnQuickAdd;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_product_icon);
            tvDiscountTag = itemView.findViewById(R.id.tv_discount_tag);
            tvBrand = itemView.findViewById(R.id.tv_brand);
            tvName = itemView.findViewById(R.id.tv_product_name);
            tvOriginalPrice = itemView.findViewById(R.id.tv_original_price);
            tvPrice = itemView.findViewById(R.id.tv_price);
            btnQuickAdd = itemView.findViewById(R.id.btn_quick_add);
        }
    }
}
