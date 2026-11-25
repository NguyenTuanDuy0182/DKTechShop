package com.ecommerce.dktechshop.ui.search;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.ecommerce.dktechshop.R;
import com.ecommerce.dktechshop.data.model.Product;
import com.ecommerce.dktechshop.ui.detail.ProductDetailActivity;
import com.ecommerce.dktechshop.ui.viewmodel.ShopViewModel;
import com.ecommerce.dktechshop.utils.NetworkMonitor;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SearchActivity extends AppCompatActivity {

    private EditText etSearchQuery;
    private MaterialButton btnClear;
    private TextView tvSearchCount;
    private LinearLayout layoutEmpty;
    private ImageView ivEmptyIcon;
    private TextView tvEmptyTitle;
    private TextView tvEmptySubtitle;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvSearchResults;
    private ProductSearchAdapter adapter;
    private ShopViewModel viewModel;

    private String currentSort = "popular";
    private List<Product> currentResults = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        MaterialButton btnBack = findViewById(R.id.btn_search_back);
        etSearchQuery = findViewById(R.id.et_search_query);
        btnClear = findViewById(R.id.btn_search_clear);
        MaterialButtonToggleGroup toggleSort = findViewById(R.id.toggle_sort);
        tvSearchCount = findViewById(R.id.tv_search_count);
        layoutEmpty = findViewById(R.id.layout_search_empty);
        ivEmptyIcon = findViewById(R.id.iv_search_empty_icon);
        tvEmptyTitle = findViewById(R.id.tv_search_empty_title);
        tvEmptySubtitle = findViewById(R.id.tv_search_empty_subtitle);
        swipeRefresh = findViewById(R.id.swipe_refresh_search);
        rvSearchResults = findViewById(R.id.rv_search_results);

        if (swipeRefresh != null) {
            swipeRefresh.setColorSchemeResources(R.color.md_theme_light_primary);
            swipeRefresh.setOnRefreshListener(() -> {
                if (!NetworkMonitor.isConnected(this)) {
                    swipeRefresh.setRefreshing(false);
                    Toast.makeText(this, NetworkMonitor.NO_INTERNET_MESSAGE, Toast.LENGTH_SHORT).show();
                    applySortAndDisplay();
                    return;
                }
                viewModel.loadProducts();
                swipeRefresh.postDelayed(() -> {
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                }, 800);
            });
        }

        adapter = new ProductSearchAdapter(product -> {
            Intent intent = new Intent(this, ProductDetailActivity.class);
            intent.putExtra(ProductDetailActivity.EXTRA_PRODUCT, product);
            intent.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, product.getId());
            startActivity(intent);
        });

        rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
        rvSearchResults.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());
        btnClear.setOnClickListener(v -> etSearchQuery.setText(""));

        viewModel = new ViewModelProvider(this).get(ShopViewModel.class);
        viewModel.getProducts().observe(this, products -> {
            if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
            currentResults = new ArrayList<>(products);
            applySortAndDisplay();
        });

        NetworkMonitor.bindOfflineBanner(this, this, findViewById(R.id.banner_no_internet), () -> {
            viewModel.loadProducts();
        });
        NetworkMonitor.getInstance(this).observe(this, connected -> applySortAndDisplay());

        etSearchQuery.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int count, int after) {
                btnClear.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                viewModel.searchProducts(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        toggleSort.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btn_sort_popular) {
                    currentSort = "popular";
                } else if (checkedId == R.id.btn_sort_price_asc) {
                    currentSort = "price_asc";
                } else if (checkedId == R.id.btn_sort_price_desc) {
                    currentSort = "price_desc";
                }
                applySortAndDisplay();
            }
        });
    }

    private void applySortAndDisplay() {
        if ("price_asc".equals(currentSort)) {
            Collections.sort(currentResults, Comparator.comparingLong(Product::getPrice));
        } else if ("price_desc".equals(currentSort)) {
            Collections.sort(currentResults, (a, b) -> Long.compare(b.getPrice(), a.getPrice()));
        } else {
            Collections.sort(currentResults, (a, b) -> Double.compare(b.getRating(), a.getRating()));
        }

        adapter.setProducts(currentResults);
        tvSearchCount.setText("Kết quả khớp: " + currentResults.size() + " sản phẩm");

        boolean isEmpty = currentResults.isEmpty();
        boolean offline = !NetworkMonitor.isConnected(this);
        if (isEmpty && offline) {
            if (ivEmptyIcon != null) ivEmptyIcon.setImageResource(R.drawable.ic_wifi_off);
            if (tvEmptyTitle != null) tvEmptyTitle.setText("Không có kết nối mạng");
            if (tvEmptySubtitle != null) tvEmptySubtitle.setText("Vui lòng kiểm tra Wi-Fi hoặc dữ liệu di động để tìm kiếm.");
        } else {
            if (ivEmptyIcon != null) ivEmptyIcon.setImageResource(R.drawable.ic_search);
            if (tvEmptyTitle != null) tvEmptyTitle.setText("Không tìm thấy thiết bị phù hợp");
            if (tvEmptySubtitle != null) tvEmptySubtitle.setText("Thử tìm kiếm với từ khóa khác");
        }
        layoutEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvSearchResults.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }
}
