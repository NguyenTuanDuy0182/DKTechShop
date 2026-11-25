package com.ecommerce.dktechshop.ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.ecommerce.dktechshop.data.model.Order;
import com.ecommerce.dktechshop.data.model.Product;
import com.ecommerce.dktechshop.data.model.Review;
import com.ecommerce.dktechshop.data.repository.ProductRepository;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class ShopViewModel extends ViewModel {
    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private final ProductRepository repository;
    private final MutableLiveData<List<Product>> products = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> selectedCategory = new MutableLiveData<>("all");
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Product> selectedProduct = new MutableLiveData<>();
    private final MutableLiveData<List<Review>> reviews = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isEligibleToReview = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> reviewSubmitted = new MutableLiveData<>(false);
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

    public LiveData<Product> getSelectedProduct() {
        return selectedProduct;
    }

    public void setSelectedProduct(Product product) {
        this.selectedProduct.setValue(product);
    }

    public void loadProductById(String productId) {
        isLoading.setValue(true);
        repository.getProductById(productId, new ProductRepository.Callback<Product>() {
            @Override
            public void onSuccess(Product result) {
                selectedProduct.setValue(result);
                isLoading.setValue(false);
            }

            @Override
            public void onError(Exception e) {
                errorMessage.setValue(e.getMessage());
                isLoading.setValue(false);
            }
        });
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

    public void setMasterProducts(List<Product> products) {
        this.masterProducts = products != null ? products : new ArrayList<>();
    }

    public void searchProducts(String query) {
        if (query == null || query.trim().isEmpty()) {
            products.setValue(new ArrayList<>(masterProducts));
            return;
        }
        List<Product> filtered = new ArrayList<>();
        for (Product p : masterProducts) {
            if (matchesProduct(p, query)) {
                filtered.add(p);
            }
        }
        products.setValue(filtered);
    }

    public static boolean matchesProduct(Product p, String query) {
        if (p == null) return false;
        if (query == null || query.trim().isEmpty()) return true;
        String cleanQuery = stripAccents(query);
        String[] tokens = cleanQuery.split("\\s+");
        String productText = buildProductSearchText(p);
        for (String token : tokens) {
            if (!productText.contains(token)) {
                return false;
            }
        }
        return true;
    }

    public static String buildProductSearchText(Product p) {
        if (p == null) return "";
        StringBuilder sb = new StringBuilder();
        if (p.getName() != null) sb.append(p.getName()).append(' ');
        if (p.getBrand() != null) sb.append(p.getBrand()).append(' ');
        if (p.getCategory() != null) sb.append(p.getCategory()).append(' ');
        if (p.getCategoryLabel() != null) sb.append(p.getCategoryLabel()).append(' ');
        if (p.getDescription() != null) sb.append(p.getDescription()).append(' ');
        if (p.getSpecs() != null) {
            for (Map.Entry<String, String> entry : p.getSpecs().entrySet()) {
                if (entry.getKey() != null) sb.append(entry.getKey()).append(' ');
                if (entry.getValue() != null) sb.append(entry.getValue()).append(' ');
            }
        }
        for (String v : p.getVariantNames()) {
            sb.append(v).append(' ');
        }
        return stripAccents(sb.toString());
    }

    public static String stripAccents(String input) {
        if (input == null) return "";
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        return DIACRITICS.matcher(normalized)
                .replaceAll("")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    public LiveData<List<Review>> getReviews() {
        return reviews;
    }

    public LiveData<Boolean> getIsEligibleToReview() {
        return isEligibleToReview;
    }

    public LiveData<Boolean> getReviewSubmitted() {
        return reviewSubmitted;
    }

    public void loadReviews(String productId) {
        repository.getReviews(productId, new ProductRepository.Callback<List<Review>>() {
            @Override
            public void onSuccess(List<Review> result) {
                reviews.setValue(result);
            }

            @Override
            public void onError(Exception e) {
                reviews.setValue(new ArrayList<>());
            }
        });
    }

    public void checkPurchaseEligibility(String productId) {
        checkPurchaseEligibility(productId, new ProductRepository.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                isEligibleToReview.setValue(result);
            }

            @Override
            public void onError(Exception e) {
                isEligibleToReview.setValue(false);
            }
        });
    }

    public void checkPurchaseEligibility(String productId, ProductRepository.Callback<Boolean> callback) {
        repository.checkUserPurchasedProduct(productId, callback);
    }

    public void checkReviewEligibility(String productId, ProductRepository.Callback<ProductRepository.ReviewEligibility> callback) {
        repository.checkReviewEligibility(productId, callback);
    }

    public void submitReview(Review review, Order order, Runnable onSuccess, Runnable onError) {
        isLoading.setValue(true);
        repository.addReview(review, order, new ProductRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                isLoading.setValue(false);
                reviewSubmitted.setValue(true);
                loadReviews(review.getProductId());
                if (review.getProductId() != null) {
                    loadProductById(review.getProductId());
                }
                if (onSuccess != null) onSuccess.run();
            }

            @Override
            public void onError(Exception e) {
                isLoading.setValue(false);
                errorMessage.setValue(e.getMessage());
                if (onError != null) onError.run();
            }
        });
    }
}
