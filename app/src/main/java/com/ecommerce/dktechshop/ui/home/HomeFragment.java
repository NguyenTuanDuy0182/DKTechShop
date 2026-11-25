package com.ecommerce.dktechshop.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.ecommerce.dktechshop.MainActivity;
import com.ecommerce.dktechshop.R;
import com.ecommerce.dktechshop.data.model.Product;
import com.ecommerce.dktechshop.data.model.ProductVariant;
import com.ecommerce.dktechshop.data.model.User;
import com.ecommerce.dktechshop.data.repository.AuthRepository;
import com.ecommerce.dktechshop.data.repository.CartRepository;
import com.ecommerce.dktechshop.ui.auth.AuthViewModel;
import com.ecommerce.dktechshop.ui.cart.CartViewModel;
import com.ecommerce.dktechshop.ui.detail.ProductDetailActivity;
import com.ecommerce.dktechshop.ui.search.SearchActivity;
import com.ecommerce.dktechshop.ui.viewmodel.ShopViewModel;
import com.ecommerce.dktechshop.utils.NetworkMonitor;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class HomeFragment extends Fragment {

    private ProductGridAdapter adapter;
    private TextView tvSectionTitle;
    private MaterialButton btnHomeSort;
    private TextView tvGreetingName;
    private ShopViewModel viewModel;
    private CircularProgressIndicator progressLoading;
    private RecyclerView rvProducts;
    private LinearLayout layoutHomeOffline;
    private SwipeRefreshLayout swipeRefreshHome;
    private CartViewModel cartViewModel;
    private AuthViewModel authViewModel;
    private String currentCategory = "all";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        cartViewModel = new ViewModelProvider(requireActivity()).get(CartViewModel.class);
        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        tvGreetingName = view.findViewById(R.id.tv_greeting_name);
        MaterialButton btnProfile = view.findViewById(R.id.btn_home_profile);
        MaterialCardView cardSearch = view.findViewById(R.id.card_search_trigger);
        MaterialCardView cardPromo = view.findViewById(R.id.card_promo_banner);
        ChipGroup chipGroup = view.findViewById(R.id.chip_group_category);
        tvSectionTitle = view.findViewById(R.id.tv_section_title);
        btnHomeSort = view.findViewById(R.id.btn_home_sort);
        rvProducts = view.findViewById(R.id.rv_products_grid);
        progressLoading = view.findViewById(R.id.progress_loading);
        layoutHomeOffline = view.findViewById(R.id.layout_home_offline);
        MaterialButton btnHomeRetry = view.findViewById(R.id.btn_home_retry);
        swipeRefreshHome = view.findViewById(R.id.swipe_refresh_home);

        if (btnHomeRetry != null) {
            btnHomeRetry.setOnClickListener(v -> {
                if (!NetworkMonitor.isConnected(requireContext())) {
                    Toast.makeText(requireContext(), NetworkMonitor.NO_INTERNET_MESSAGE, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (viewModel != null) {
                    viewModel.filterByCategory(currentCategory);
                }
            });
        }

        if (swipeRefreshHome != null) {
            swipeRefreshHome.setColorSchemeResources(
                    R.color.md_theme_light_primary,
                    R.color.green_accent,
                    R.color.md_theme_light_error
            );
            swipeRefreshHome.setOnRefreshListener(() -> {
                if (!NetworkMonitor.isConnected(requireContext())) {
                    swipeRefreshHome.setRefreshing(false);
                    Toast.makeText(requireContext(), NetworkMonitor.NO_INTERNET_MESSAGE, Toast.LENGTH_SHORT).show();
                    updateOfflineView();
                    return;
                }
                if (viewModel != null) {
                    viewModel.filterByCategory(currentCategory);
                }
            });
        }

        // Hiển thị ngay tên người dùng từ Firebase Auth hoặc bộ nhớ đệm AuthRepository
        updateGreeting();

        // Lắng nghe dữ liệu người dùng thời gian thực từ AuthViewModel
        authViewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null && user.getName() != null && !user.getName().trim().isEmpty()) {
                tvGreetingName.setText(user.getName());
            }
        });

        btnProfile.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToTab(R.id.nav_profile);
            }
        });

        cardSearch.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), SearchActivity.class);
            startActivity(intent);
        });

        cardPromo.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), ProductDetailActivity.class);
            intent.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, "prod_logi_gpx2");
            startActivity(intent);
        });

        adapter = new ProductGridAdapter(new ProductGridAdapter.OnProductClickListener() {
            @Override
            public void onProductClick(Product product) {
                Intent intent = new Intent(requireContext(), ProductDetailActivity.class);
                intent.putExtra(ProductDetailActivity.EXTRA_PRODUCT, product);
                intent.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, product.getId());
                startActivity(intent);
            }

            @Override
            public void onQuickAddClick(Product product) {
                if (!NetworkMonitor.isConnected(requireContext())) {
                    Toast.makeText(requireContext(), NetworkMonitor.NO_INTERNET_MESSAGE, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (product.computeTotalStock() <= 0) {
                    Toast.makeText(requireContext(), "Sản phẩm hiện đã hết hàng", Toast.LENGTH_SHORT).show();
                    return;
                }
                ProductVariant available = product.findFirstAvailableVariant();
                String variant = (available != null && available.getName() != null && !available.getName().isEmpty())
                        ? available.getName()
                        : "Mặc định";
                cartViewModel.addToCart(product, variant, 1, new CartRepository.ActionCallback() {
                    @Override
                    public void onSuccess() {
                        if (isAdded()) {
                            Toast.makeText(requireContext(),
                                    "Đã thêm " + product.getName() + " (" + variant + ") vào giỏ!",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(String error) {
                        if (isAdded()) {
                            Toast.makeText(requireContext(),
                                    error != null ? error : "Không thể thêm vào giỏ hàng",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        rvProducts.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        rvProducts.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(ShopViewModel.class);

        if (btnHomeSort != null) {
            btnHomeSort.setOnClickListener(v -> showSortBottomSheet());
        }

        viewModel.getSortOption().observe(getViewLifecycleOwner(), this::updateSortButtonUI);

        viewModel.getProducts().observe(getViewLifecycleOwner(), list -> {
            if (swipeRefreshHome != null) {
                swipeRefreshHome.setRefreshing(false);
            }
            adapter.setProducts(list);
            if (tvSectionTitle != null) {
                tvSectionTitle.setText("Thiết bị nổi bật (" + (list != null ? list.size() : 0) + ")");
            }
            updateOfflineView();
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (swipeRefreshHome != null) {
                swipeRefreshHome.setRefreshing(false);
            }
            updateOfflineView();
        });

        NetworkMonitor.getInstance(requireContext()).observe(getViewLifecycleOwner(), connected -> {
            boolean online = Boolean.TRUE.equals(connected);
            updateOfflineView();
            if (online && (viewModel.getProducts().getValue() == null || viewModel.getProducts().getValue().isEmpty())) {
                viewModel.filterByCategory(currentCategory);
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            boolean loading = Boolean.TRUE.equals(isLoading);
            if (progressLoading != null) {
                progressLoading.setVisibility(loading && (swipeRefreshHome == null || !swipeRefreshHome.isRefreshing()) ? View.VISIBLE : View.GONE);
            }
            if (!loading && swipeRefreshHome != null) {
                swipeRefreshHome.setRefreshing(false);
            }
            if (!loading) {
                updateOfflineView();
            } else if (rvProducts != null && (swipeRefreshHome == null || !swipeRefreshHome.isRefreshing())) {
                rvProducts.setVisibility(View.GONE);
                if (layoutHomeOffline != null) layoutHomeOffline.setVisibility(View.GONE);
            }
        });

        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            String category = "all";
            if (checkedIds.contains(R.id.chip_mouse)) {
                category = "mouse";
            } else if (checkedIds.contains(R.id.chip_headphone)) {
                category = "headphone";
            } else if (checkedIds.contains(R.id.chip_keyboard)) {
                category = "keyboard";
            } else if (checkedIds.contains(R.id.chip_under35)) {
                category = "under35";
            }
            currentCategory = category;
            if (!NetworkMonitor.isConnected(requireContext())) {
                Toast.makeText(requireContext(), NetworkMonitor.NO_INTERNET_MESSAGE, Toast.LENGTH_SHORT).show();
                updateOfflineView();
                return;
            }
            viewModel.filterByCategory(category);
        });
    }

    private void updateSortButtonUI(ShopViewModel.SortOption option) {
        if (btnHomeSort == null || !isAdded()) return;
        ShopViewModel.SortOption safe = option != null ? option : ShopViewModel.SortOption.DEFAULT;
        boolean isActive = safe != ShopViewModel.SortOption.DEFAULT;
        switch (safe) {
            case PRICE_ASC:
                btnHomeSort.setText("Giá tăng");
                break;
            case PRICE_DESC:
                btnHomeSort.setText("Giá giảm");
                break;
            case SOLD_DESC:
                btnHomeSort.setText("Bán chạy");
                break;
            case SOLD_ASC:
                btnHomeSort.setText("Mua ít");
                break;
            default:
                btnHomeSort.setText("Sắp xếp");
                break;
        }
        int bgColor = ContextCompat.getColor(requireContext(),
                isActive ? R.color.md_theme_light_primaryContainer : R.color.md_theme_light_surfaceContainerHigh);
        int textColor = ContextCompat.getColor(requireContext(),
                isActive ? R.color.md_theme_light_onPrimaryContainer : R.color.md_theme_light_onSurface);
        int iconColor = ContextCompat.getColor(requireContext(),
                isActive ? R.color.md_theme_light_onPrimaryContainer : R.color.md_theme_light_onSurfaceVariant);
        btnHomeSort.setBackgroundTintList(android.content.res.ColorStateList.valueOf(bgColor));
        btnHomeSort.setTextColor(textColor);
        btnHomeSort.setIconTint(android.content.res.ColorStateList.valueOf(iconColor));
    }

    private void showSortBottomSheet() {
        if (!isAdded()) return;
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext(), R.style.Theme_DKTechShop_BottomSheetDialog);
        View sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_sort, null);
        dialog.setContentView(sheetView);

        MaterialButton btnClose = sheetView.findViewById(R.id.btn_sort_sheet_close);
        LinearLayout layoutOptions = sheetView.findViewById(R.id.layout_sort_options);
        btnClose.setOnClickListener(v -> dialog.dismiss());

        ShopViewModel.SortOption current = viewModel.getSortOption().getValue();
        if (current == null) current = ShopViewModel.SortOption.DEFAULT;

        ShopViewModel.SortOption[] options = new ShopViewModel.SortOption[]{
                ShopViewModel.SortOption.DEFAULT,
                ShopViewModel.SortOption.PRICE_ASC,
                ShopViewModel.SortOption.PRICE_DESC,
                ShopViewModel.SortOption.SOLD_DESC,
                ShopViewModel.SortOption.SOLD_ASC
        };
        String[] titles = new String[]{
                "Mặc định (Nổi bật)",
                "Giá: Thấp đến cao",
                "Giá: Cao đến thấp",
                "Lượt mua: Bán chạy nhất",
                "Lượt mua: Ít đến nhiều"
        };
        String[] subtitles = new String[]{
                "Hiển thị thứ tự gợi ý của cửa hàng",
                "Ưu tiên sản phẩm có mức giá tốt nhất",
                "Ưu tiên sản phẩm cao cấp trước",
                "Ưu tiên thiết bị được mua nhiều nhất",
                "Ưu tiên thiết bị có lượt mua thấp nhất"
        };

        float density = getResources().getDisplayMetrics().density;
        int padH = (int) (14 * density);
        int padV = (int) (12 * density);
        int marginB = (int) (8 * density);

        for (int i = 0; i < options.length; i++) {
            final ShopViewModel.SortOption opt = options[i];
            boolean isSelected = opt == current;

            MaterialCardView card = new MaterialCardView(requireContext());
            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardLp.setMargins(0, 0, 0, marginB);
            card.setLayoutParams(cardLp);
            card.setRadius(14 * density);
            card.setCardElevation(0f);
            card.setStrokeWidth((int) (1 * density));
            card.setStrokeColor(ContextCompat.getColor(requireContext(),
                    isSelected ? R.color.md_theme_light_primary : R.color.md_theme_light_outlineVariant));
            card.setCardBackgroundColor(ContextCompat.getColor(requireContext(),
                    isSelected ? R.color.md_theme_light_secondaryContainer : R.color.md_theme_light_surfaceContainerLowest));
            card.setClickable(true);
            card.setFocusable(true);

            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(padH, padV, padH, padV);

            LinearLayout textCol = new LinearLayout(requireContext());
            textCol.setOrientation(LinearLayout.VERTICAL);
            textCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvTitle = new TextView(requireContext());
            tvTitle.setText(titles[i]);
            tvTitle.setTextSize(13f);
            tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            tvTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_light_onSurface));
            textCol.addView(tvTitle);

            TextView tvSub = new TextView(requireContext());
            tvSub.setText(subtitles[i]);
            tvSub.setTextSize(11f);
            tvSub.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_light_onSurfaceVariant));
            tvSub.setPadding(0, (int) (2 * density), 0, 0);
            textCol.addView(tvSub);

            RadioButton rb = new RadioButton(requireContext());
            rb.setChecked(isSelected);
            rb.setClickable(false);
            rb.setFocusable(false);

            row.addView(textCol);
            row.addView(rb);
            card.addView(row);

            card.setOnClickListener(v -> {
                viewModel.setSortOption(opt);
                dialog.dismiss();
            });

            layoutOptions.addView(card);
        }

        dialog.show();
    }

    private void updateOfflineView() {
        if (!isAdded()) return;
        boolean offline = !NetworkMonitor.isConnected(requireContext());
        boolean isEmpty = viewModel == null || viewModel.getProducts().getValue() == null || viewModel.getProducts().getValue().isEmpty();
        boolean showOffline = offline && isEmpty;
        if (layoutHomeOffline != null) {
            layoutHomeOffline.setVisibility(showOffline ? View.VISIBLE : View.GONE);
        }
        if (rvProducts != null) {
            rvProducts.setVisibility(showOffline ? View.GONE : View.VISIBLE);
        }
    }

    private void updateGreeting() {
        if (tvGreetingName == null)
            return;

        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
        User cachedUser = AuthRepository.getInstance().getCachedUser();
        if (fbUser != null && fbUser.getDisplayName() != null && !fbUser.getDisplayName().trim().isEmpty()) {
            tvGreetingName.setText(fbUser.getDisplayName());
        } else if (cachedUser != null && cachedUser.getName() != null && !cachedUser.getName().trim().isEmpty()) {
            tvGreetingName.setText(cachedUser.getName());
        } else {
            tvGreetingName.setText("Người dùng");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateGreeting();
    }
}
