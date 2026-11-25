package com.ecommerce.dktechshop.ui.detail;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
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
import com.ecommerce.dktechshop.utils.CurrencyUtils;
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
    private TextView tvStock;
    private TextView tvQty;
    private MaterialButton btnMinus;
    private MaterialButton btnPlus;
    private MaterialButton btnConfirm;

    public ProductVariantBottomSheetDialog(Product product, boolean isBuyNow, OnVariantActionListener listener) {
        this(product, isBuyNow, null, listener);
    }

    public ProductVariantBottomSheetDialog(Product product, boolean isBuyNow, @Nullable ProductVariant initialVariant, OnVariantActionListener listener) {
        this.product = product;
        this.isBuyNow = isBuyNow;
        this.listener = listener;
        if (product != null) {
            if (initialVariant != null) {
                this.selectedVariant = initialVariant;
            } else {
                this.selectedVariant = product.findFirstAvailableVariant();
            }
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
        tvStock = view.findViewById(R.id.tv_sheet_stock);
        MaterialButton btnClose = view.findViewById(R.id.btn_sheet_close);
        ChipGroup chipGroup = view.findViewById(R.id.chip_group_sheet_variants);
        btnMinus = view.findViewById(R.id.btn_qty_minus);
        btnPlus = view.findViewById(R.id.btn_qty_plus);
        tvQty = view.findViewById(R.id.tv_sheet_qty);
        btnConfirm = view.findViewById(R.id.btn_sheet_confirm);

        if (product != null) {
            tvName.setText(product.getName());

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
                int variantStock = product.resolveVariantStock(variant.getName());
                String title = variant.getName();
                if (variant.getExtraPrice() > 0) {
                    title += " (+" + CurrencyUtils.formatVND(variant.getExtraPrice()) + ")";
                }
                if (product.hasExplicitVariantStock() && variantStock <= 0) {
                    title += " (Hết hàng)";
                }
                chip.setText(title);
                chip.setCheckable(true);
                chip.setClickable(true);
                chip.setCheckedIconVisible(false);

                boolean initialSelected = selectedVariant != null && variant.getName().equalsIgnoreCase(selectedVariant.getName());
                chip.setChecked(initialSelected);
                applyVariantChipStyle(chip, variant, initialSelected, variantStock <= 0);

                chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    int currentVarStock = product.resolveVariantStock(variant.getName());
                    applyVariantChipStyle(chip, variant, isChecked, currentVarStock <= 0);
                    if (isChecked) {
                        selectedVariant = variant;
                        updateVariantDisplay();
                    }
                });
                chipGroup.addView(chip);
            }
        }

        updateVariantDisplay();

        btnClose.setOnClickListener(v -> dismiss());

        btnMinus.setOnClickListener(v -> {
            if (quantity > 1) {
                quantity--;
                updateVariantDisplay();
            }
        });

        btnPlus.setOnClickListener(v -> {
            int maxStock = getSelectedVariantStock();
            if (product != null && quantity < maxStock) {
                quantity++;
                updateVariantDisplay();
            }
        });

        btnConfirm.setOnClickListener(v -> {
            if (getSelectedVariantStock() <= 0 || quantity <= 0) {
                return;
            }
            if (listener != null) {
                listener.onVariantSelected(product, selectedVariant, quantity, isBuyNow);
            }
            dismiss();
        });
    }

    private int getSelectedVariantStock() {
        if (product == null) return 0;
        if (selectedVariant != null) {
            return product.resolveVariantStock(selectedVariant.getName());
        }
        return product.computeTotalStock();
    }

    private void applyVariantChipStyle(Chip chip, ProductVariant variant, boolean isSelected, boolean isOutOfStock) {
        if (!isAdded()) return;
        int primary = ContextCompat.getColor(requireContext(), R.color.md_theme_light_primary);
        int onPrimary = ContextCompat.getColor(requireContext(), R.color.md_theme_light_onPrimary);
        int surfaceWhite = ContextCompat.getColor(requireContext(), R.color.md_theme_light_surfaceContainerLowest);
        int onSurface = ContextCompat.getColor(requireContext(), R.color.md_theme_light_onSurface);
        float strokeWidth = 1.5f * getResources().getDisplayMetrics().density;

        chip.setChipStrokeColor(ColorStateList.valueOf(primary));
        chip.setChipStrokeWidth(strokeWidth);
        chip.setChipBackgroundColor(ColorStateList.valueOf(isSelected ? primary : surfaceWhite));
        chip.setTextColor(isSelected ? onPrimary : onSurface);
        chip.setTypeface(null, isSelected ? Typeface.BOLD : Typeface.NORMAL);
        chip.setAlpha(isOutOfStock && !isSelected ? 0.6f : 1.0f);

        if (variant.getColorHex() != null && !variant.getColorHex().trim().isEmpty()) {
            try {
                int color = Color.parseColor(variant.getColorHex().trim());
                GradientDrawable dot = new GradientDrawable();
                dot.setShape(GradientDrawable.OVAL);
                dot.setSize(36, 36);
                dot.setColor(color);
                dot.setStroke(isSelected ? 3 : 2, isSelected ? Color.WHITE : 0x33000000);
                chip.setChipIcon(dot);
                chip.setChipIconVisible(true);
            } catch (Exception ignored) {}
        }
    }

    private void updateVariantDisplay() {
        if (product == null || !isAdded()) return;
        long extra = (selectedVariant != null) ? selectedVariant.getExtraPrice() : 0L;
        long unitPrice = product.getPrice() + extra;
        int varStock = getSelectedVariantStock();
        String variantName = (selectedVariant != null && selectedVariant.getName() != null)
                ? selectedVariant.getName() : "Mặc định";

        if (tvPrice != null) {
            tvPrice.setText(CurrencyUtils.formatVND(unitPrice));
        }

        if (varStock <= 0) {
            quantity = 0;
            if (tvStock != null) {
                tvStock.setText("Kho (" + variantName + "): Tạm hết hàng");
                tvStock.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_light_error));
            }
            if (tvQty != null) tvQty.setText("0");
            if (btnMinus != null) btnMinus.setEnabled(false);
            if (btnPlus != null) btnPlus.setEnabled(false);
            if (btnConfirm != null) {
                btnConfirm.setEnabled(false);
                btnConfirm.setText("Màu này đã hết hàng");
                btnConfirm.setIcon(null);
            }
            return;
        }

        if (quantity < 1) {
            quantity = 1;
        } else if (quantity > varStock) {
            quantity = varStock;
        }

        if (tvStock != null) {
            tvStock.setText("Kho (" + variantName + "): " + varStock + " sản phẩm");
            tvStock.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_light_onSurfaceVariant));
        }
        if (tvQty != null) {
            tvQty.setText(String.valueOf(quantity));
        }
        if (btnMinus != null) {
            btnMinus.setEnabled(quantity > 1);
        }
        if (btnPlus != null) {
            btnPlus.setEnabled(quantity < varStock);
        }

        long total = unitPrice * quantity;
        if (btnConfirm != null) {
            btnConfirm.setEnabled(true);
            String action = isBuyNow ? "Mua ngay" : "Thêm vào giỏ";
            btnConfirm.setText(action + " • " + CurrencyUtils.formatVND(total));
            if (isBuyNow) {
                btnConfirm.setIcon(null);
            } else {
                btnConfirm.setIconResource(R.drawable.ic_shopping_cart);
            }
        }
    }
}
