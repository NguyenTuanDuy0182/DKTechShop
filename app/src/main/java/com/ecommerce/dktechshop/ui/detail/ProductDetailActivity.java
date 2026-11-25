package com.ecommerce.dktechshop.ui.detail;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.ecommerce.dktechshop.MainActivity;
import com.ecommerce.dktechshop.R;
import com.ecommerce.dktechshop.data.model.Product;
import com.ecommerce.dktechshop.data.model.ProductVariant;
import com.ecommerce.dktechshop.data.model.Review;
import com.ecommerce.dktechshop.data.repository.CartRepository;
import com.ecommerce.dktechshop.data.repository.ProductRepository;
import com.ecommerce.dktechshop.ui.cart.CartViewModel;
import com.ecommerce.dktechshop.ui.viewmodel.ShopViewModel;
import com.ecommerce.dktechshop.utils.CurrencyUtils;
import com.ecommerce.dktechshop.utils.NetworkMonitor;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ProductDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PRODUCT = "extra_product";
    public static final String EXTRA_PRODUCT_ID = "extra_product_id";

    private Product product;
    private TextView tvCartBadge;
    private CartViewModel cartViewModel;
    private ShopViewModel shopViewModel;
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvReviewsEmpty;
    private LinearLayout layoutReviewsListContainer;
    private String loadedReviewsProductId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        shopViewModel = new ViewModelProvider(this).get(ShopViewModel.class);
        cartViewModel = new ViewModelProvider(this).get(CartViewModel.class);
        tvCartBadge = findViewById(R.id.tv_detail_cart_badge);

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

        swipeRefresh = findViewById(R.id.swipe_refresh_product_detail);
        Runnable reloadProduct = () -> {
            String prodId = (product != null) ? product.getId() : getIntent().getStringExtra(EXTRA_PRODUCT_ID);
            if (prodId != null && !prodId.trim().isEmpty()) {
                shopViewModel.loadProductById(prodId);
                shopViewModel.loadReviews(prodId);
            }
        };

        NetworkMonitor.bindOfflineBanner(this, this, findViewById(R.id.banner_no_internet), reloadProduct);

        if (swipeRefresh != null) {
            swipeRefresh.setColorSchemeResources(R.color.md_theme_light_primary);
            swipeRefresh.setOnRefreshListener(() -> {
                if (!NetworkMonitor.isConnected(this)) {
                    swipeRefresh.setRefreshing(false);
                    Toast.makeText(this, NetworkMonitor.NO_INTERNET_MESSAGE, Toast.LENGTH_SHORT).show();
                    return;
                }
                reloadProduct.run();
                swipeRefresh.postDelayed(() -> {
                    if (swipeRefresh != null) {
                        swipeRefresh.setRefreshing(false);
                    }
                }, 800);
            });
        }

        shopViewModel.getSelectedProduct().observe(this, p -> {
            if (swipeRefresh != null) {
                swipeRefresh.setRefreshing(false);
            }
            if (p != null) {
                this.product = p;
                setupViews();
            }
        });

        shopViewModel.getReviews().observe(this, reviews -> {
            if (swipeRefresh != null) {
                swipeRefresh.setRefreshing(false);
            }
            renderReviews(reviews);
        });

        Product extraProduct;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            extraProduct = getIntent().getSerializableExtra(EXTRA_PRODUCT, Product.class);
        } else {
            extraProduct = (Product) getIntent().getSerializableExtra(EXTRA_PRODUCT);
        }

        if (extraProduct != null) {
            shopViewModel.setSelectedProduct(extraProduct);
            if (extraProduct.getId() != null && !extraProduct.getId().trim().isEmpty() && NetworkMonitor.isConnected(this)) {
                shopViewModel.loadProductById(extraProduct.getId());
            }
        } else {
            String productId = getIntent().getStringExtra(EXTRA_PRODUCT_ID);
            if (productId != null) {
                shopViewModel.loadProductById(productId);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (product != null && product.getId() != null && !product.getId().trim().isEmpty()
                && NetworkMonitor.isConnected(this)) {
            shopViewModel.loadProductById(product.getId());
        }
    }

    private void setupViews() {
        if (product == null) return;

        MaterialButton btnBack = findViewById(R.id.btn_detail_back);
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
        TabLayout tabLayoutDetails = findViewById(R.id.tab_layout_details);
        LinearLayout layoutSpecsContainer = findViewById(R.id.layout_specs_container);
        LinearLayout layoutWarrantyContainer = findViewById(R.id.layout_warranty_container);

        MaterialButton btnAddToCart = findViewById(R.id.btn_detail_add_to_cart);
        MaterialButton btnBuyNow = findViewById(R.id.btn_detail_buy_now);

        tvBrandBadge.setText(product.getBrand());
        tvName.setText(product.getName());
        tvCategory.setText(product.getCategoryLabel());
        View layoutRating = findViewById(R.id.layout_detail_rating);
        if (layoutRating != null) {
            if (product.getReviewCount() > 0) {
                layoutRating.setVisibility(View.VISIBLE);
                tvRating.setText(String.valueOf(product.getRating()));
                tvReviews.setText("(" + product.getReviewCount() + " đánh giá)");
            } else {
                layoutRating.setVisibility(View.GONE);
            }
        }
        int totalStock = product.computeTotalStock();
        if (totalStock > 0) {
            tvStock.setText("Còn sẵn " + totalStock + " thiết bị tại kho");
            tvStock.setTextColor(ContextCompat.getColor(this, R.color.md_theme_light_onSurfaceVariant));
        } else {
            tvStock.setText("Tạm hết hàng");
            tvStock.setTextColor(ContextCompat.getColor(this, R.color.md_theme_light_error));
        }
        tvPrice.setText(CurrencyUtils.formatVND(product.getPrice()));
        if (product.getOriginalPrice() > product.getPrice() && product.getDiscountPercent() > 0) {
            tvOriginalPrice.setVisibility(View.VISIBLE);
            tvDiscount.setVisibility(View.VISIBLE);
            tvOriginalPrice.setText(CurrencyUtils.formatVND(product.getOriginalPrice()));
            tvOriginalPrice.setPaintFlags(tvOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            tvDiscount.setText("Tiết kiệm " + product.getDiscountPercent() + "%");
        } else {
            tvOriginalPrice.setVisibility(View.GONE);
            tvDiscount.setVisibility(View.GONE);
        }

        if (product.getThumbnailUrl() != null && !product.getThumbnailUrl().trim().isEmpty()) {
            ivHeroIcon.setImageTintList(null);
            ivHeroIcon.clearColorFilter();
            Glide.with(this).load(product.getThumbnailUrl()).into(ivHeroIcon);
        } else {
            ivHeroIcon.setImageTintList(ContextCompat.getColorStateList(this, R.color.md_theme_light_primary));
            if ("mouse".equalsIgnoreCase(product.getCategory())) {
                ivHeroIcon.setImageResource(R.drawable.ic_mouse);
            } else if ("headphone".equalsIgnoreCase(product.getCategory())) {
                ivHeroIcon.setImageResource(R.drawable.ic_headset);
            } else {
                ivHeroIcon.setImageResource(R.drawable.ic_devices);
            }
        }

        btnBack.setOnClickListener(v -> finish());

        btnCart.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("navigate_tab", R.id.nav_cart);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        chipGroupVariants.removeAllViews();
        int primaryColor = ContextCompat.getColor(this, R.color.md_theme_light_primary);
        int whiteBg = ContextCompat.getColor(this, R.color.md_theme_light_surfaceContainerLowest);
        int onSurfaceColor = ContextCompat.getColor(this, R.color.md_theme_light_onSurface);
        float strokeWidthPx = 1.5f * getResources().getDisplayMetrics().density;

        for (ProductVariant v : product.getVariants()) {
            Chip chip = new Chip(this);
            int varStock = product.resolveVariantStock(v.getName());
            String title = v.getName();
            if (v.getExtraPrice() > 0) {
                title += " (+" + CurrencyUtils.formatVND(v.getExtraPrice()) + ")";
            }
            if (product.hasExplicitVariantStock()) {
                title += (varStock > 0) ? (" • Còn " + varStock) : " • Hết hàng";
            }
            chip.setText(title);
            chip.setCheckable(false);
            chip.setChipBackgroundColor(ColorStateList.valueOf(whiteBg));
            chip.setChipStrokeColor(ColorStateList.valueOf(primaryColor));
            chip.setChipStrokeWidth(strokeWidthPx);
            chip.setTextColor(onSurfaceColor);
            chip.setAlpha(product.hasExplicitVariantStock() && varStock <= 0 ? 0.6f : 1.0f);

            if (v.getColorHex() != null && !v.getColorHex().trim().isEmpty()) {
                try {
                    int color = Color.parseColor(v.getColorHex().trim());
                    GradientDrawable dot = new GradientDrawable();
                    dot.setShape(GradientDrawable.OVAL);
                    dot.setSize(36, 36);
                    dot.setColor(color);
                    dot.setStroke(2, 0x33000000);
                    chip.setChipIcon(dot);
                    chip.setChipIconVisible(true);
                } catch (Exception ignored) {}
            }

            chip.setOnClickListener(view -> openVariantSheet(false, v));
            chipGroupVariants.addView(chip);
        }

        populateTable(layoutSpecsContainer, product.getSpecs());

        Map<String, String> warrantyInfo = new LinkedHashMap<>();
        warrantyInfo.put("Thời hạn bảo hành", "24 tháng chính hãng");
        warrantyInfo.put("Chính sách đổi mới", "1 đổi 1 trong 30 ngày (lỗi NSX)");
        warrantyInfo.put("Hình thức bảo hành", "Bảo hành điện tử theo Serial Number");
        warrantyInfo.put("Điều kiện bảo hành", "Còn nguyên tem, không rơi vỡ, vào nước");
        warrantyInfo.put("Địa điểm bảo hành", "Trung tâm DKTechShop & TTBH ủy quyền");
        populateTable(layoutWarrantyContainer, warrantyInfo);

        tabLayoutDetails.clearOnTabSelectedListeners();
        tabLayoutDetails.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                boolean isSpecs = tab.getPosition() == 0;
                layoutSpecsContainer.setVisibility(isSpecs ? View.VISIBLE : View.GONE);
                layoutWarrantyContainer.setVisibility(isSpecs ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        boolean inStock = totalStock > 0;
        btnAddToCart.setEnabled(inStock);
        btnBuyNow.setEnabled(inStock);
        btnBuyNow.setText(inStock ? "Mua ngay" : "Hết hàng");
        btnAddToCart.setOnClickListener(v -> openVariantSheet(false, null));
        btnBuyNow.setOnClickListener(v -> openVariantSheet(true, null));

        // Thiết lập khu vực Đánh giá từ khách hàng
        TextView tvReviewsSummaryRating = findViewById(R.id.tv_reviews_summary_rating);
        TextView tvReviewsSummaryCount = findViewById(R.id.tv_reviews_summary_count);
        MaterialButton btnWriteReview = findViewById(R.id.btn_write_review);
        tvReviewsEmpty = findViewById(R.id.tv_reviews_empty);
        layoutReviewsListContainer = findViewById(R.id.layout_reviews_list_container);

        if (tvReviewsSummaryRating != null) {
            tvReviewsSummaryRating.setText(String.valueOf(product.getRating()));
        }
        if (tvReviewsSummaryCount != null) {
            tvReviewsSummaryCount.setText("• " + product.getReviewCount() + " đánh giá");
        }

        if (btnWriteReview != null) {
            btnWriteReview.setOnClickListener(v -> {
                if (!NetworkMonitor.isConnected(this)) {
                    Toast.makeText(this, NetworkMonitor.NO_INTERNET_MESSAGE, Toast.LENGTH_SHORT).show();
                    return;
                }
                shopViewModel.checkReviewEligibility(product.getId(), new ProductRepository.Callback<ProductRepository.ReviewEligibility>() {
                    @Override
                    public void onSuccess(ProductRepository.ReviewEligibility eligibility) {
                        if (eligibility != null && eligibility.status == ProductRepository.ReviewEligibility.Status.ELIGIBLE
                                && eligibility.eligibleOrder != null) {
                            String variant = eligibility.eligibleItem != null && eligibility.eligibleItem.getVariant() != null
                                    ? eligibility.eligibleItem.getVariant() : "";
                            ReviewDialogHelper.showReviewDialog(
                                    ProductDetailActivity.this,
                                    product.getId(),
                                    product.getName(),
                                    variant,
                                    eligibility.eligibleOrder,
                                    review -> {
                                        shopViewModel.loadReviews(product.getId());
                                        shopViewModel.loadProductById(product.getId());
                                    }
                            );
                        } else if (eligibility != null && eligibility.status == ProductRepository.ReviewEligibility.Status.ALREADY_REVIEWED) {
                            new MaterialAlertDialogBuilder(ProductDetailActivity.this)
                                    .setTitle("Đã đánh giá")
                                    .setMessage("Bạn đã đánh giá sản phẩm này cho đơn hàng đã giao. Mỗi đơn hàng chỉ được đánh giá 1 lần duy nhất.")
                                    .setPositiveButton("Đã hiểu", null)
                                    .show();
                        } else {
                            new MaterialAlertDialogBuilder(ProductDetailActivity.this)
                                    .setTitle("Chưa thể đánh giá")
                                    .setMessage("Bạn chỉ có thể viết đánh giá sau khi đơn hàng chứa sản phẩm này đã được giao thành công.")
                                    .setPositiveButton("Đã hiểu", null)
                                    .show();
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(ProductDetailActivity.this, "Không thể kiểm tra điều kiện đánh giá", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }

        if (product.getId() != null && !product.getId().equals(loadedReviewsProductId)) {
            loadedReviewsProductId = product.getId();
            shopViewModel.loadReviews(product.getId());
        }
    }

    private void openVariantSheet(boolean isBuyNow, @Nullable ProductVariant initialVariant) {
        if (product == null) return;
        ProductVariantBottomSheetDialog sheet = new ProductVariantBottomSheetDialog(product, isBuyNow, initialVariant,
                (p, variant, qty, buyNow) -> {
                    if (!NetworkMonitor.isConnected(this)) {
                        Toast.makeText(this, NetworkMonitor.NO_INTERNET_MESSAGE, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String variantName = variant != null ? variant.getName() : "";
                    cartViewModel.addToCart(p, variantName, qty, new CartRepository.ActionCallback() {
                        @Override
                        public void onSuccess() {
                            if (buyNow) {
                                Intent intent = new Intent(ProductDetailActivity.this, MainActivity.class);
                                intent.putExtra("navigate_tab", R.id.nav_cart);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(ProductDetailActivity.this,
                                        "Đã thêm " + qty + " sản phẩm (" + variantName + ") vào giỏ!",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(String error) {
                            Toast.makeText(ProductDetailActivity.this,
                                    error != null ? error : "Không thể thêm vào giỏ hàng",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                });
        sheet.show(getSupportFragmentManager(), "variant_sheet");
    }

    private void populateTable(LinearLayout container, Map<String, String> data) {
        container.removeAllViews();
        if (data == null) return;
        int idx = 0;
        for (Map.Entry<String, String> entry : data.entrySet()) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(32, 20, 32, 20);
            row.setBackgroundColor(ContextCompat.getColor(this,
                    idx % 2 == 0 ? R.color.md_theme_light_surfaceContainerLowest
                            : R.color.md_theme_light_surfaceContainerLow));

            TextView tvKey = new TextView(this);
            tvKey.setText(entry.getKey());
            tvKey.setTextSize(12);
            tvKey.setTextColor(ContextCompat.getColor(this, R.color.md_theme_light_onSurfaceVariant));
            tvKey.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvVal = new TextView(this);
            tvVal.setText(entry.getValue());
            tvVal.setTextSize(12);
            tvVal.setTextColor(ContextCompat.getColor(this, R.color.md_theme_light_onSurface));
            tvVal.setTypeface(null, android.graphics.Typeface.BOLD);

            row.addView(tvKey);
            row.addView(tvVal);
            container.addView(row);
            idx++;
        }
    }

    private void renderReviews(List<Review> reviewList) {
        if (layoutReviewsListContainer == null) return;
        layoutReviewsListContainer.removeAllViews();

        if (reviewList == null || reviewList.isEmpty()) {
            if (tvReviewsEmpty != null) tvReviewsEmpty.setVisibility(View.VISIBLE);
            return;
        }

        if (tvReviewsEmpty != null) tvReviewsEmpty.setVisibility(View.GONE);

        for (Review r : reviewList) {
            View itemView = LayoutInflater.from(this).inflate(R.layout.item_review, layoutReviewsListContainer, false);

            TextView tvAvatarLetter = itemView.findViewById(R.id.tv_review_avatar_letter);
            TextView tvUserName = itemView.findViewById(R.id.tv_review_user_name);
            TextView tvDate = itemView.findViewById(R.id.tv_review_date);
            TextView tvVariant = itemView.findViewById(R.id.tv_review_variant);
            TextView tvComment = itemView.findViewById(R.id.tv_review_comment);

            ImageView[] stars = new ImageView[]{
                    itemView.findViewById(R.id.iv_item_star_1),
                    itemView.findViewById(R.id.iv_item_star_2),
                    itemView.findViewById(R.id.iv_item_star_3),
                    itemView.findViewById(R.id.iv_item_star_4),
                    itemView.findViewById(R.id.iv_item_star_5)
            };

            String name = r.getUserName() != null && !r.getUserName().trim().isEmpty() ? r.getUserName().trim() : "Khách hàng";
            tvUserName.setText(name);
            if (!name.isEmpty()) {
                tvAvatarLetter.setText(name.substring(0, 1).toUpperCase());
            }

            tvDate.setText(r.getCreatedAt());

            if (r.getVariant() != null && !r.getVariant().trim().isEmpty()) {
                tvVariant.setText("Phân loại: " + r.getVariant().trim());
                tvVariant.setVisibility(View.VISIBLE);
            } else {
                tvVariant.setVisibility(View.GONE);
            }

            tvComment.setText(r.getComment());

            int starCount = Math.round(r.getRating());
            for (int i = 0; i < 5; i++) {
                if (i < starCount) {
                    stars[i].setImageResource(R.drawable.ic_star);
                } else {
                    stars[i].setImageResource(R.drawable.ic_star_outline);
                }
            }

            layoutReviewsListContainer.addView(itemView);
        }
    }
}
