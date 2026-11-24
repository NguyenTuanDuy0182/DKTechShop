package com.ecommerce.dktechshop.ui.detail;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.ecommerce.dktechshop.R;
import com.ecommerce.dktechshop.data.model.Product;
import com.ecommerce.dktechshop.data.model.ProductVariant;
import com.ecommerce.dktechshop.data.repository.MockRepository;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

public class ProductVariantBottomSheetDialog extends BottomSheetDialogFragment {

    public interface OnVariantActionListener {
        void onVariantSelected(Product product, ProductVariant variant, int quantity, boolean isBuyNow);
    }

    private final Product product;
    private final boolean isBuyNow;
    private final OnVariantActionListener listener;
    private ProductVariant selectedVariant;
    private int quantity = 1;

    private TextView tvPrice;
    private MaterialButton btnConfirm;

    public ProductVariantBottomSheetDialog(Product product, boolean isBuyNow, OnVariantActionListener listener) {
        this.product = product;
        this.isBuyNow = isBuyNow;
        this.listener = listener;
        if (product != null && product.getVariants() != null && !product.getVariants().isEmpty()) {
            this.selectedVariant = product.getVariants().get(0);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_variant, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageView ivIcon = view.findViewById(R.id.iv_sheet_icon);
        TextView tvName = view.findViewById(R.id.tv_sheet_name);
        tvPrice = view.findViewById(R.id.tv_sheet_price);
        TextView tvStock = view.findViewById(R.id.tv_sheet_stock);
        MaterialButton btnClose = view.findViewById(R.id.btn_sheet_close);
        ChipGroup chipGroup = view.findViewById(R.id.chip_group_sheet_variants);
        MaterialButton btnMinus = view.findViewById(R.id.btn_qty_minus);
        MaterialButton btnPlus = view.findViewById(R.id.btn_qty_plus);
        TextView tvQty = view.findViewById(R.id.tv_sheet_qty);
        btnConfirm = view.findViewById(R.id.btn_sheet_confirm);

        if (product != null) {
            tvName.setText(product.getName());
            tvStock.setText("Kho: " + product.getStock() + " sản phẩm");

            if (product.getThumbnailUrl() != null && !product.getThumbnailUrl().trim().isEmpty()) {
                ivIcon.setImageTintList(null);
                ivIcon.clearColorFilter();
                Glide.with(this).load(product.getThumbnailUrl()).into(ivIcon);
            } else {
                ivIcon.setImageTintList(ContextCompat.getColorStateList(requireContext(), R.color.md_theme_light_primary));
                if ("mouse".equalsIgnoreCase(product.getCategory())) {
                    ivIcon.setImageResource(R.drawable.ic_mouse);
                } else if ("headphone".equalsIgnoreCase(product.getCategory())) {
                    ivIcon.setImageResource(R.drawable.ic_headset);
                } else {
                    ivIcon.setImageResource(R.drawable.ic_devices);
                }
            }

            chipGroup.removeAllViews();
            for (ProductVariant variant : product.getVariants()) {
                Chip chip = new Chip(requireContext());
                String title = variant.getName();
                if (variant.getExtraPrice() > 0) {
                    title += " (+" + MockRepository.formatVND(variant.getExtraPrice()) + ")";
                }
                chip.setText(title);
                chip.setCheckable(true);
                chip.setClickable(true);

                if (variant.getColorHex() != null && !variant.getColorHex().trim().isEmpty()) {
                    try {
                        int color = Color.parseColor(variant.getColorHex().trim());
                        GradientDrawable dot = new GradientDrawable();
                        dot.setShape(GradientDrawable.OVAL);
                        dot.setSize(36, 36);
                        dot.setColor(color);
                        dot.setStroke(2, 0x33000000);
                        chip.setChipIcon(dot);
                        chip.setChipIconVisible(true);
                    } catch (Exception ignored) {}
                }

                if (selectedVariant != null && variant.getName().equals(selectedVariant.getName())) {
                    chip.setChecked(true);
                }

                chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        selectedVariant = variant;
                        updatePriceDisplay();
                    }
                });
                chipGroup.addView(chip);
            }
        }

        updatePriceDisplay();

        btnClose.setOnClickListener(v -> dismiss());

        btnMinus.setOnClickListener(v -> {
            if (quantity > 1) {
                quantity--;
                tvQty.setText(String.valueOf(quantity));
                updatePriceDisplay();
            }
        });

        btnPlus.setOnClickListener(v -> {
            if (product != null && quantity < product.getStock()) {
                quantity++;
                tvQty.setText(String.valueOf(quantity));
                updatePriceDisplay();
            }
        });

        btnConfirm.setOnClickListener(v -> {
            if (listener != null) {
                listener.onVariantSelected(product, selectedVariant, quantity, isBuyNow);
            }
            dismiss();
        });
    }

    private void updatePriceDisplay() {
        if (product == null) return;
        long extra = (selectedVariant != null) ? selectedVariant.getExtraPrice() : 0L;
        long unitPrice = product.getPrice() + extra;
        long total = unitPrice * quantity;

        if (tvPrice != null) {
            tvPrice.setText(MockRepository.formatVND(unitPrice));
        }

        if (btnConfirm != null) {
            String action = isBuyNow ? "Mua ngay" : "Thêm vào giỏ";
            btnConfirm.setText(action + " • " + MockRepository.formatVND(total));
            if (isBuyNow) {
                btnConfirm.setIconResource(R.drawable.ic_bolt);
            } else {
                btnConfirm.setIconResource(R.drawable.ic_shopping_cart);
            }
        }
    }
}
