package com.ecommerce.dktechshop.ui.detail;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.ecommerce.dktechshop.MainActivity;
import com.ecommerce.dktechshop.R;
import com.ecommerce.dktechshop.data.model.Product;
import com.ecommerce.dktechshop.data.repository.MockRepository;
import com.ecommerce.dktechshop.ui.cart.CartViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.Map;

public class ProductDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PRODUCT_ID = "extra_product_id";

    private Product product;
    private MaterialButton btnFavorite;
    private TextView tvCartBadge;
    private CartViewModel cartViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        cartViewModel = new ViewModelProvider(this).get(CartViewModel.class);

        String productId = getIntent().getStringExtra(EXTRA_PRODUCT_ID);
        if (productId != null) {
            product = MockRepository.getInstance().getProductById(productId);
        } else {
            product = MockRepository.getInstance().getProducts().get(0);
        }

        setupViews();
    }

    private void setupViews() {
        MaterialButton btnBack = findViewById(R.id.btn_detail_back);
        btnFavorite = findViewById(R.id.btn_detail_favorite);
        MaterialButton btnCart = findViewById(R.id.btn_detail_cart);
        tvCartBadge = findViewById(R.id.tv_detail_cart_badge);

        TextView tvBrandBadge = findViewById(R.id.tv_detail_brand_badge);
        ImageView ivHeroIcon = findViewById(R.id.iv_detail_hero_icon);
        TextView tvStock = findViewById(R.id.tv_detail_stock);

        TextView tvCategory = findViewById(R.id.tv_detail_category);
        TextView tvRating = findViewById(R.id.tv_detail_rating);
        TextView tvReviews = findViewById(R.id.tv_detail_reviews);
        TextView tvName = findViewById(R.id.tv_detail_name);
        TextView tvPrice = findViewById(R.id.tv_detail_price);
        TextView tvOriginalPrice = findViewById(R.id.tv_detail_original_price);
        TextView tvDiscount = findViewById(R.id.tv_detail_discount);

        ChipGroup chipGroupVariants = findViewById(R.id.chip_group_detail_variants);
        LinearLayout layoutSpecsContainer = findViewById(R.id.layout_specs_container);

        MaterialButton btnAddToCart = findViewById(R.id.btn_detail_add_to_cart);
        MaterialButton btnBuyNow = findViewById(R.id.btn_detail_buy_now);

        // Bind data
        tvBrandBadge.setText(product.getBrand());
        tvName.setText(product.getName());
        tvCategory.setText(product.getCategoryLabel());
        tvRating.setText(String.valueOf(product.getRating()));
        tvReviews.setText("(" + product.getReviewCount() + " đánh giá)");
        tvStock.setText("Còn sẵn " + product.getStock() + " thiết bị tại kho");
        tvPrice.setText(MockRepository.formatVND(product.getPrice()));
        tvOriginalPrice.setText(MockRepository.formatVND(product.getOriginalPrice()));
        tvOriginalPrice.setPaintFlags(tvOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        tvDiscount.setText("Tiết kiệm " + product.getDiscountPercent() + "%");

        if ("mouse".equalsIgnoreCase(product.getCategory())) {
            ivHeroIcon.setImageResource(R.drawable.ic_mouse);
        } else if ("headphone".equalsIgnoreCase(product.getCategory())) {
            ivHeroIcon.setImageResource(R.drawable.ic_headset);
        } else {
            ivHeroIcon.setImageResource(R.drawable.ic_devices);
        }

        updateFavoriteIcon();

        // Lắng nghe realtime cartCount từ CartViewModel
        cartViewModel.getCartCount().observe(this, count -> {
            if (tvCartBadge != null) {
                if (count != null && count > 0) {
                    tvCartBadge.setVisibility(View.VISIBLE);
                    tvCartBadge.setText(String.valueOf(count));
                } else {
                    tvCartBadge.setVisibility(View.GONE);
                }
            }
        });

        btnBack.setOnClickListener(v -> finish());

        btnFavorite.setOnClickListener(v -> {
            MockRepository.getInstance().toggleFavorite(product.getId());
            updateFavoriteIcon();
            String msg = MockRepository.getInstance().isFavorite(product.getId())
                    ? "Đã lưu vào danh sách yêu thích"
                    : "Đã bỏ khỏi danh sách yêu thích";
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });

        btnCart.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("navigate_tab", R.id.nav_cart);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        // Variant preview chips
        chipGroupVariants.removeAllViews();
        for (String v : product.getVariants()) {
            Chip chip = new Chip(this);
            chip.setText(v);
            chip.setCheckable(false);
            chip.setOnClickListener(view -> openVariantSheet(false));
            chipGroupVariants.addView(chip);
        }

        // Specs table rows
        layoutSpecsContainer.removeAllViews();
        int idx = 0;
        for (Map.Entry<String, String> entry : product.getSpecs().entrySet()) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(32, 20, 32, 20);
            if (idx % 2 == 0) {
                row.setBackgroundColor(ContextCompat.getColor(this, R.color.md_theme_light_surfaceContainerLowest));
            } else {
                row.setBackgroundColor(ContextCompat.getColor(this, R.color.md_theme_light_surfaceContainerLow));
            }

            TextView tvKey = new TextView(this);
            tvKey.setText(entry.getKey());
            tvKey.setTextSize(12);
            tvKey.setTextColor(ContextCompat.getColor(this, R.color.md_theme_light_onSurfaceVariant));
            LinearLayout.LayoutParams lpKey = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            tvKey.setLayoutParams(lpKey);

            TextView tvVal = new TextView(this);
            tvVal.setText(entry.getValue());
            tvVal.setTextSize(12);
            tvVal.setTextColor(ContextCompat.getColor(this, R.color.md_theme_light_onSurface));
            tvVal.setTypeface(null, android.graphics.Typeface.BOLD);

            row.addView(tvKey);
            row.addView(tvVal);
            layoutSpecsContainer.addView(row);
            idx++;
        }

        btnAddToCart.setOnClickListener(v -> openVariantSheet(false));
        btnBuyNow.setOnClickListener(v -> openVariantSheet(true));
    }

    private void updateFavoriteIcon() {
        boolean fav = MockRepository.getInstance().isFavorite(product.getId());
        if (fav) {
            btnFavorite.setIconResource(R.drawable.ic_favorite);
            btnFavorite.setIconTint(ContextCompat.getColorStateList(this, R.color.md_theme_light_error));
        } else {
            btnFavorite.setIconResource(R.drawable.ic_favorite_border);
            btnFavorite.setIconTint(ContextCompat.getColorStateList(this, R.color.md_theme_light_onSurfaceVariant));
        }
    }

    private void openVariantSheet(boolean isBuyNow) {
        ProductVariantBottomSheetDialog sheet = new ProductVariantBottomSheetDialog(product, isBuyNow,
                (p, variant, qty, buyNow) -> {
                    cartViewModel.addToCart(p, variant, qty);
                    if (buyNow) {
                        Intent intent = new Intent(this, MainActivity.class);
                        intent.putExtra("navigate_tab", R.id.nav_cart);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(this, "Đã thêm " + qty + " sản phẩm vào giỏ!", Toast.LENGTH_SHORT).show();
                    }
                });
        sheet.show(getSupportFragmentManager(), "variant_sheet");
    }
}
