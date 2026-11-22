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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ecommerce.dktechshop.R;
import com.ecommerce.dktechshop.data.model.Product;
import com.ecommerce.dktechshop.data.repository.MockRepository;
import com.ecommerce.dktechshop.ui.detail.ProductDetailActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.util.List;

public class SearchActivity extends AppCompatActivity {

    private EditText etSearchQuery;
    private MaterialButton btnClear;
    private TextView tvSearchCount;
    private LinearLayout layoutEmpty;
    private RecyclerView rvSearchResults;
    private ProductSearchAdapter adapter;

    private String currentSort = "popular";

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
            intent.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, product.getId());
            startActivity(intent);
        });

        rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
        rvSearchResults.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());

        btnClear.setOnClickListener(v -> etSearchQuery.setText(""));

        etSearchQuery.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int count, int after) {
                btnClear.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                performSearchAndSort();
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
                performSearchAndSort();
            }
        });

        performSearchAndSort();
    }

    private void performSearchAndSort() {
        String query = etSearchQuery.getText() != null ? etSearchQuery.getText().toString() : "";
        List<Product> results = MockRepository.getInstance().searchAndSort(query, currentSort);
        adapter.setProducts(results);

        tvSearchCount.setText("Kết quả khớp: " + results.size() + " sản phẩm");

        if (results.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvSearchResults.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvSearchResults.setVisibility(View.VISIBLE);
        }
    }
}
