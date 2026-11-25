package com.ecommerce.dktechshop.ui.cart;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.ecommerce.dktechshop.data.model.Address;
import com.ecommerce.dktechshop.data.model.CartItem;
import com.ecommerce.dktechshop.data.model.Order;
import com.ecommerce.dktechshop.data.model.OrderItem;
import com.ecommerce.dktechshop.data.model.Product;
import com.ecommerce.dktechshop.data.repository.CartRepository;
import com.ecommerce.dktechshop.data.repository.OrderRepository;
import com.ecommerce.dktechshop.data.repository.ProductRepository;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class CartViewModel extends ViewModel {

    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;

    private final MutableLiveData<List<CartItem>> cartItems = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Set<String>> selectedItemIdsLiveData = new MutableLiveData<>(new HashSet<>());
    private final MutableLiveData<Double> totalAmount = new MutableLiveData<>(0.0);
    private final MutableLiveData<Integer> cartCount = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> selectedItemCount = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> selectedTotalQuantity = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();
    private final MutableLiveData<Order> checkoutSuccessOrder = new MutableLiveData<>();

    private final Set<String> selectedItemIds = new HashSet<>();
    private final Set<String> knownItemIds = new HashSet<>();
    private ListenerRegistration cartListenerRegistration;

    public CartViewModel() {
        this(CartRepository.getInstance(), OrderRepository.getInstance());
    }

    public CartViewModel(CartRepository cartRepository, OrderRepository orderRepository) {
        this.cartRepository = cartRepository;
        this.orderRepository = orderRepository;
        startListeningCart();
    }

    public LiveData<List<CartItem>> getCartItems() {
        return cartItems;
    }

    public LiveData<Set<String>> getSelectedItemIds() {
        return selectedItemIdsLiveData;
    }

    public LiveData<Double> getTotalAmount() {
        return totalAmount;
    }

    public LiveData<Integer> getCartCount() {
        return cartCount;
    }

    public LiveData<Integer> getSelectedItemCount() {
        return selectedItemCount;
    }

    public LiveData<Integer> getSelectedTotalQuantity() {
        return selectedTotalQuantity;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<String> getSuccessMessage() {
        return successMessage;
    }

    public LiveData<Order> getCheckoutSuccessOrder() {
        return checkoutSuccessOrder;
    }

    public void clearCheckoutSuccess() {
        checkoutSuccessOrder.setValue(null);
    }

    public void clearErrorMessage() {
        errorMessage.setValue(null);
    }

    /**
     * Bắt đầu lắng nghe thay đổi giỏ hàng từ Firestore thời gian thực.
     */
    public void startListeningCart() {
        if (cartListenerRegistration != null) {
            cartListenerRegistration.remove();
        }

        cartListenerRegistration = cartRepository.listenCartItems(new CartRepository.CartListener() {
            @Override
            public void onCartChanged(List<CartItem> items) {
                List<CartItem> safeItems = items != null ? items : new ArrayList<>();
                cartItems.setValue(safeItems);

                int count = 0;
                for (CartItem item : safeItems) {
                    count += item.getQuantity();
                }
                cartCount.setValue(count);

                syncSelectionWithCart(safeItems);
            }

            @Override
            public void onError(String error) {
                errorMessage.setValue(error);
            }
        });
    }

    /**
     * Giữ nguyên trạng thái chọn của các sản phẩm cũ, tự động chọn sản phẩm mới thêm vào giỏ,
     * và dọn bỏ ID các sản phẩm đã xóa khỏi giỏ.
     */
    private void syncSelectionWithCart(List<CartItem> items) {
        Set<String> currentIds = new HashSet<>();
        for (CartItem item : items) {
            if (item.getItemId() != null) {
                currentIds.add(item.getItemId());
            }
        }

        selectedItemIds.retainAll(currentIds);
        knownItemIds.retainAll(currentIds);

        for (String id : currentIds) {
            if (!knownItemIds.contains(id)) {
                knownItemIds.add(id);
                selectedItemIds.add(id);
            }
        }

        recalculateSelection(items);
    }

    private void recalculateSelection(List<CartItem> items) {
        long selectedTotal = calculateSelectedTotal(items, selectedItemIds);
        int selItemCount = 0;
        int selQty = 0;
        if (items != null) {
            for (CartItem item : items) {
                if (selectedItemIds.contains(item.getItemId())) {
                    selItemCount++;
                    selQty += item.getQuantity();
                }
            }
        }
        selectedItemIdsLiveData.setValue(new HashSet<>(selectedItemIds));
        selectedItemCount.setValue(selItemCount);
        selectedTotalQuantity.setValue(selQty);
        totalAmount.setValue((double) selectedTotal);
    }

    public void setItemSelected(String itemId, boolean selected) {
        if (itemId == null) return;
        if (selected) {
            selectedItemIds.add(itemId);
        } else {
            selectedItemIds.remove(itemId);
        }
        recalculateSelection(cartItems.getValue());
    }

    public void selectAll(boolean selectAll) {
        selectedItemIds.clear();
        List<CartItem> items = cartItems.getValue();
        if (selectAll && items != null) {
            for (CartItem item : items) {
                if (item.getItemId() != null) {
                    selectedItemIds.add(item.getItemId());
                }
            }
        }
        recalculateSelection(items);
    }

    public List<CartItem> getSelectedCartItems() {
        return filterSelectedItems(cartItems.getValue(), selectedItemIds);
    }

    public static List<CartItem> filterSelectedItems(List<CartItem> items, Set<String> selectedIds) {
        List<CartItem> selected = new ArrayList<>();
        if (items == null || selectedIds == null || selectedIds.isEmpty()) return selected;
        for (CartItem item : items) {
            if (item != null && selectedIds.contains(item.getItemId())) {
                selected.add(item);
            }
        }
        return selected;
    }

    public static long calculateSelectedTotal(List<CartItem> items, Set<String> selectedIds) {
        long total = 0L;
        for (CartItem item : filterSelectedItems(items, selectedIds)) {
            total += item.getPrice() * item.getQuantity();
        }
        return total;
    }

    public static boolean isValidPhone(String phone) {
        if (phone == null) return false;
        String cleaned = phone.trim().replaceAll("[\\s.-]", "");
        return cleaned.matches("^(0|\\+84)[0-9]{9,10}$");
    }

    /**
     * Thêm sản phẩm vào giỏ hàng Firestore.
     */
    public void addToCart(Product product, String variant, int qty) {
        if (product == null || qty <= 0) return;

        isLoading.setValue(true);
        String safeVariant = (variant != null && !variant.trim().isEmpty()) ? variant.trim() : "Mặc định";
        String itemId = CartRepository.generateItemId(product.getId(), safeVariant);

        CartItem item = new CartItem(
                itemId,
                product.getId(),
                product.getName(),
                product.getCategory(),
                product.getPrice(),
                safeVariant,
                qty,
                product.getThumbnailUrl()
        );

        cartRepository.upsertCartItem(item, new CartRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                isLoading.setValue(false);
                successMessage.setValue("Đã thêm vào giỏ hàng!");
            }

            @Override
            public void onFailure(String error) {
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }

    /**
     * Cập nhật số lượng item với delta (+1 hoặc -1).
     */
    public void updateQuantity(String itemId, int delta) {
        if (itemId == null || itemId.isEmpty()) return;

        cartRepository.updateQuantity(itemId, delta, new CartRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                // Realtime snapshot listener sẽ tự động cập nhật UI
            }

            @Override
            public void onFailure(String error) {
                errorMessage.setValue(error);
            }
        });
    }

    /**
     * Xóa một sản phẩm khỏi giỏ hàng.
     */
    public void deleteCartItem(String itemId) {
        if (itemId == null || itemId.isEmpty()) return;

        cartRepository.deleteCartItem(itemId, new CartRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                // Realtime snapshot listener sẽ tự động cập nhật UI
            }

            @Override
            public void onFailure(String error) {
                errorMessage.setValue(error);
            }
        });
    }

    /**
     * Xóa toàn bộ giỏ hàng.
     */
    public void clearCart() {
        cartRepository.clearCart(new CartRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                // Realtime snapshot listener sẽ tự động cập nhật UI
            }

            @Override
            public void onFailure(String error) {
                errorMessage.setValue(error);
            }
        });
    }

    /**
     * Đặt hàng & Thanh toán các sản phẩm đã được chọn bằng Batch Write an toàn.
     * Chỉ tạo đơn và xóa khỏi giỏ những sản phẩm được tick chọn.
     */
    public static String generateOrderId() {
        return "TG-2026-" + (1000 + (int) (Math.random() * 9000));
    }

    public void checkout(Address shippingAddress, String paymentMethod) {
        checkout(shippingAddress, paymentMethod, "", "", null);
    }

    public void checkout(Address shippingAddress, String paymentMethod, String recipientName, String recipientPhone) {
        checkout(shippingAddress, paymentMethod, recipientName, recipientPhone, null);
    }

    public void checkout(Address shippingAddress, String paymentMethod, String recipientName, String recipientPhone, String preGeneratedOrderId) {
        List<CartItem> selectedItems = getSelectedCartItems();
        if (selectedItems.isEmpty()) {
            errorMessage.setValue("Vui lòng chọn ít nhất 1 sản phẩm để đặt hàng");
            return;
        }

        String addressDetail = (shippingAddress != null && shippingAddress.getDetail() != null && !shippingAddress.getDetail().trim().isEmpty())
                ? shippingAddress.getDetail().trim()
                : "";
        if (addressDetail.isEmpty()) {
            errorMessage.setValue("Vui lòng chọn hoặc thêm địa chỉ giao hàng");
            return;
        }

        if ((recipientPhone == null || recipientPhone.trim().isEmpty()) && shippingAddress.getPhone() != null) {
            recipientPhone = shippingAddress.getPhone();
        }
        if (!isValidPhone(recipientPhone)) {
            errorMessage.setValue("Vui lòng cập nhật số điện thoại nhận hàng hợp lệ trước khi đặt hàng");
            return;
        }
        recipientPhone = recipientPhone.trim();

        String payment = (paymentMethod != null && !paymentMethod.trim().isEmpty())
                ? paymentMethod
                : "COD";

        List<OrderItem> orderItems = new ArrayList<>();
        long total = 0L;
        for (CartItem ci : selectedItems) {
            orderItems.add(new OrderItem(
                    ci.getProductId(),
                    ci.getProductName(),
                    ci.getVariant(),
                    ci.getPrice(),
                    ci.getQuantity()
            ));
            total += ci.getPrice() * ci.getQuantity();
        }

        String orderId = (preGeneratedOrderId != null && !preGeneratedOrderId.trim().isEmpty())
                ? preGeneratedOrderId.trim()
                : generateOrderId();
        String createdAt = new SimpleDateFormat("dd/MM/yyyy • HH:mm", Locale.getDefault()).format(new Date());

        Order newOrder = new Order(
                orderId,
                "", // Sẽ được cập nhật từ FirebaseUser trong OrderRepository
                createdAt,
                "Đang xử lý",
                payment,
                addressDetail,
                recipientName != null ? recipientName : "",
                recipientPhone != null ? recipientPhone : "",
                total,
                orderItems
        );

        isLoading.setValue(true);
        orderRepository.placeOrderWithBatch(newOrder, selectedItems, new OrderRepository.OrderCallback() {
            @Override
            public void onSuccess(Order order) {
                isLoading.setValue(false);
                for (CartItem ci : selectedItems) {
                    selectedItemIds.remove(ci.getItemId());
                    knownItemIds.remove(ci.getItemId());
                }
                recalculateSelection(cartItems.getValue());
                checkoutSuccessOrder.setValue(order);
                successMessage.setValue("Đặt hàng thành công (" + order.getOrderId() + ")!");
            }

            @Override
            public void onFailure(String error) {
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }

    /**
     * Thêm tất cả sản phẩm của một đơn hàng cũ vào lại giỏ hàng (Mua lại) qua Firestore.
     */
    public void reorder(Order order, CartRepository.ActionCallback callback) {
        if (order == null || order.getItems() == null || order.getItems().isEmpty()) {
            if (callback != null) callback.onFailure("Không có sản phẩm để mua lại");
            return;
        }

        List<OrderItem> items = order.getItems();
        AtomicInteger remaining = new AtomicInteger(items.size());
        AtomicBoolean failed = new AtomicBoolean(false);

        for (OrderItem oi : items) {
            String productId = oi.getProductId();
            Runnable upsertFallback = () -> upsertReorderedItem(oi, "", "", remaining, failed, callback);

            if (productId != null && !productId.trim().isEmpty()) {
                ProductRepository.getInstance().getProductById(productId, new ProductRepository.Callback<Product>() {
                    @Override
                    public void onSuccess(Product p) {
                        String thumb = (p != null) ? p.getThumbnailUrl() : "";
                        String category = (p != null) ? p.getCategory() : "";
                        upsertReorderedItem(oi, category, thumb, remaining, failed, callback);
                    }

                    @Override
                    public void onError(Exception e) {
                        upsertFallback.run();
                    }
                });
            } else {
                upsertFallback.run();
            }
        }
    }

    private void upsertReorderedItem(OrderItem oi, String category, String thumb,
                                     AtomicInteger remaining, AtomicBoolean failed,
                                     CartRepository.ActionCallback callback) {
        String itemId = CartRepository.generateItemId(oi.getProductId(), oi.getVariant());
        CartItem ci = new CartItem(
                itemId,
                oi.getProductId(),
                oi.getProductName(),
                category,
                oi.getPrice(),
                oi.getVariant(),
                oi.getQuantity(),
                thumb
        );
        cartRepository.upsertCartItem(ci, new CartRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                if (remaining.decrementAndGet() == 0 && !failed.get() && callback != null) {
                    callback.onSuccess();
                }
            }

            @Override
            public void onFailure(String error) {
                if (failed.compareAndSet(false, true) && callback != null) {
                    callback.onFailure(error);
                }
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (cartListenerRegistration != null) {
            cartListenerRegistration.remove();
            cartListenerRegistration = null;
        }
    }
}
