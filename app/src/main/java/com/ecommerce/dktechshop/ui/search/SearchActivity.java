package com.ecommerce.dktechshop.ui.search;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ecommerce.dktechshop.R;
import com.ecommerce.dktechshop.data.model.Product;
import com.ecommerce.dktechshop.ui.detail.ProductDetailActivity;
import com.ecommerce.dktechshop.ui.viewmodel.ShopViewModel;
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
        rvSearchResults = findViewById(R.id.rv_search_results);

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
            currentResults = new ArrayList<>(products);
            applySortAndDisplay();
        });

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
        layoutEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvSearchResults.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }
}
