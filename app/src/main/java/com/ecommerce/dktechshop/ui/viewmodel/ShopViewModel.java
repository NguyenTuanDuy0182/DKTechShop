package com.ecommerce.dktechshop.ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.ecommerce.dktechshop.data.model.Product;
import com.ecommerce.dktechshop.data.repository.ProductRepository;

import java.util.ArrayList;
import java.util.List;

public class ShopViewModel extends ViewModel {
    private final ProductRepository repository;
    private final MutableLiveData<List<Product>> products = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> selectedCategory = new MutableLiveData<>("all");
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private List<Product> masterProducts = new ArrayList<>();

    public ShopViewModel() {
        this(ProductRepository.getInstance());
    }

    public ShopViewModel(ProductRepository repository) {
        this.repository = repository;
        loadProducts();
    }

    public LiveData<List<Product>> getProducts() {
        return products;
    }

    public LiveData<String> getSelectedCategory() {
        return selectedCategory;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void loadProducts() {
        isLoading.setValue(true);
        repository.getAllProducts(new ProductRepository.Callback<List<Product>>() {
            @Override
            public void onSuccess(List<Product> result) {
                masterProducts = result;
                products.setValue(result);
                isLoading.setValue(false);
            }

            @Override
            public void onError(Exception e) {
                errorMessage.setValue(e.getMessage());
                isLoading.setValue(false);
            }
        });
    }

    public void filterByCategory(String category) {
        selectedCategory.setValue(category);
        isLoading.setValue(true);
        repository.getProductsByCategory(category, new ProductRepository.Callback<List<Product>>() {
            @Override
            public void onSuccess(List<Product> result) {
                products.setValue(result);
                isLoading.setValue(false);
            }

            @Override
            public void onError(Exception e) {
                errorMessage.setValue(e.getMessage());
                isLoading.setValue(false);
            }
        });
    }

    public void searchProducts(String query) {
        if (query == null || query.trim().isEmpty()) {
            products.setValue(new ArrayList<>(masterProducts));
            return;
        }
        String q = query.toLowerCase().trim();
        List<Product> filtered = new ArrayList<>();
        for (Product p : masterProducts) {
            boolean matchName = p.getName() != null && p.getName().toLowerCase().contains(q);
            boolean matchBrand = p.getBrand() != null && p.getBrand().toLowerCase().contains(q);
            if (matchName || matchBrand) {
                filtered.add(p);
            }
        }
        products.setValue(filtered);
    }
}
