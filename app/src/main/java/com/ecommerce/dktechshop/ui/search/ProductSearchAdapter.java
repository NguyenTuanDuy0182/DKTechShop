package com.ecommerce.dktechshop.ui.search;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ecommerce.dktechshop.R;
import com.ecommerce.dktechshop.data.model.Product;
import com.ecommerce.dktechshop.data.repository.MockRepository;

import java.util.ArrayList;
import java.util.List;

public class ProductSearchAdapter extends RecyclerView.Adapter<ProductSearchAdapter.ViewHolder> {

    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    private List<Product> productList = new ArrayList<>();
    private final OnProductClickListener listener;

    public ProductSearchAdapter(OnProductClickListener listener) {
        this.listener = listener;
    }

    public void setProducts(List<Product> products) {
        this.productList = products;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product_search, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product p = productList.get(position);
        holder.tvBrand.setText(p.getBrand() + " • " + p.getCategoryLabel());
        holder.tvName.setText(p.getName());
        holder.tvPrice.setText(MockRepository.formatVND(p.getPrice()));
        holder.tvRating.setText(String.valueOf(p.getRating()));

        if ("mouse".equalsIgnoreCase(p.getCategory())) {
            holder.ivIcon.setImageResource(R.drawable.ic_mouse);
        } else if ("headphone".equalsIgnoreCase(p.getCategory())) {
            holder.ivIcon.setImageResource(R.drawable.ic_headset);
        } else {
            holder.ivIcon.setImageResource(R.drawable.ic_devices);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onProductClick(p);
        });
    }

    @Override
    public int getItemCount() {
        return productList != null ? productList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvBrand;
        TextView tvName;
        TextView tvRating;
        TextView tvPrice;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_search_product_icon);
            tvBrand = itemView.findViewById(R.id.tv_search_brand);
            tvName = itemView.findViewById(R.id.tv_search_name);
            tvRating = itemView.findViewById(R.id.tv_search_rating);
            tvPrice = itemView.findViewById(R.id.tv_search_price);
        }
    }
}
