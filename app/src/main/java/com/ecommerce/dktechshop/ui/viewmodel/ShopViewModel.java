package com.ecommerce.dktechshop.ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

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
    public enum SortOption {
        DEFAULT,
        PRICE_ASC,
        PRICE_DESC,
        SOLD_DESC,
        SOLD_ASC
    }

    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private final ProductRepository repository;
    private final MutableLiveData<List<Product>> products = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<SortOption> sortOption = new MutableLiveData<>(SortOption.DEFAULT);
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Product> selectedProduct = new MutableLiveData<>();
    private final MutableLiveData<List<Review>> reviews = new MutableLiveData<>(new ArrayList<>());
    private List<Product> masterProducts = new ArrayList<>();
    private List<Product> currentCategoryProducts = new ArrayList<>();
    private SortOption currentSort = SortOption.DEFAULT;

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

    public LiveData<SortOption> getSortOption() {
        return sortOption;
    }

    public void setSortOption(SortOption option) {
        currentSort = option != null ? option : SortOption.DEFAULT;
        sortOption.setValue(currentSort);
        products.setValue(sortProducts(currentCategoryProducts, currentSort));
    }

    public static List<Product> sortProducts(List<Product> source, SortOption option) {
        List<Product> sorted = source != null ? new ArrayList<>(source) : new ArrayList<>();
        if (option == null || option == SortOption.DEFAULT || sorted.size() <= 1) {
            return sorted;
        }
        switch (option) {
            case PRICE_ASC:
                sorted.sort((a, b) -> Long.compare(a.getPrice(), b.getPrice()));
                break;
            case PRICE_DESC:
                sorted.sort((a, b) -> Long.compare(b.getPrice(), a.getPrice()));
                break;
            case SOLD_DESC:
                sorted.sort((a, b) -> {
                    int cmp = Integer.compare(b.getSoldCount(), a.getSoldCount());
                    return cmp != 0 ? cmp : Long.compare(a.getPrice(), b.getPrice());
                });
                break;
            case SOLD_ASC:
                sorted.sort((a, b) -> {
                    int cmp = Integer.compare(a.getSoldCount(), b.getSoldCount());
                    return cmp != 0 ? cmp : Long.compare(a.getPrice(), b.getPrice());
                });
                break;
            default:
                break;
        }
        return sorted;
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
                masterProducts = result != null ? new ArrayList<>(result) : new ArrayList<>();
                currentCategoryProducts = new ArrayList<>(masterProducts);
                products.setValue(sortProducts(currentCategoryProducts, currentSort));
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
        isLoading.setValue(true);
        repository.getProductsByCategory(category, new ProductRepository.Callback<List<Product>>() {
            @Override
            public void onSuccess(List<Product> result) {
                currentCategoryProducts = result != null ? new ArrayList<>(result) : new ArrayList<>();
                products.setValue(sortProducts(currentCategoryProducts, currentSort));
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

    public void checkReviewEligibility(String productId, ProductRepository.Callback<ProductRepository.ReviewEligibility> callback) {
        repository.checkReviewEligibility(productId, callback);
    }
}
