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
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CartViewModel extends ViewModel {

    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;

    private final MutableLiveData<List<CartItem>> cartItems = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Double> totalAmount = new MutableLiveData<>(0.0);
    private final MutableLiveData<Integer> cartCount = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();
    private final MutableLiveData<Order> checkoutSuccessOrder = new MutableLiveData<>();
    private final MutableLiveData<List<Order>> orders = new MutableLiveData<>(new ArrayList<>());

    private ListenerRegistration cartListenerRegistration;
    private ListenerRegistration orderListenerRegistration;

    public CartViewModel() {
        this(CartRepository.getInstance(), OrderRepository.getInstance());
    }

    public CartViewModel(CartRepository cartRepository, OrderRepository orderRepository) {
        this.cartRepository = cartRepository;
        this.orderRepository = orderRepository;
        startListeningCart();
        startListeningOrders();
    }

    public LiveData<List<Order>> getOrders() {
        return orders;
    }

    public void startListeningOrders() {
        if (orderListenerRegistration != null) {
            orderListenerRegistration.remove();
        }
        orderListenerRegistration = orderRepository.listenUserOrders(new OrderRepository.OrderListCallback() {
            @Override
            public void onSuccess(List<Order> list) {
                orders.setValue(list != null ? list : new ArrayList<>());
            }

            @Override
            public void onFailure(String error) {
                errorMessage.setValue(error);
            }
        });
    }

    public LiveData<List<CartItem>> getCartItems() {
        return cartItems;
    }

    public LiveData<Double> getTotalAmount() {
        return totalAmount;
    }

    public LiveData<Integer> getCartCount() {
        return cartCount;
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
                cartItems.setValue(items);

                double total = 0.0;
                int count = 0;
                if (items != null) {
                    for (CartItem item : items) {
                        total += (double) item.getPrice() * item.getQuantity();
                        count += item.getQuantity();
                    }
                }
                totalAmount.setValue(total);
                cartCount.setValue(count);
            }

            @Override
            public void onError(String error) {
                errorMessage.setValue(error);
            }
        });
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
     * Tính năng 5: Đặt hàng & Thanh toán bằng Batch Write an toàn.
     * Tạo đơn hàng, giảm tồn kho và dọn sạch giỏ hàng trên Firestore.
     */
    public void checkout(Address shippingAddress, String paymentMethod) {
        List<CartItem> items = cartItems.getValue();
        if (items == null || items.isEmpty()) {
            errorMessage.setValue("Giỏ hàng của bạn đang trống");
            return;
        }

        String addressDetail = (shippingAddress != null && shippingAddress.getDetail() != null && !shippingAddress.getDetail().trim().isEmpty())
                ? shippingAddress.getDetail()
                : "Địa chỉ nhận hàng";

        String payment = (paymentMethod != null && !paymentMethod.trim().isEmpty())
                ? paymentMethod
                : "COD";

        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem ci : items) {
            orderItems.add(new OrderItem(
                    ci.getProductId(),
                    ci.getProductName(),
                    ci.getVariant(),
                    ci.getPrice(),
                    ci.getQuantity()
            ));
        }

        Double totalVal = totalAmount.getValue();
        long total = totalVal != null ? totalVal.longValue() : 0L;

        String orderId = "TG-2026-" + (1000 + (int) (Math.random() * 9000));
        String createdAt = new SimpleDateFormat("dd/MM/yyyy • HH:mm", Locale.getDefault()).format(new Date());

        Order newOrder = new Order(
                orderId,
                "", // Sẽ được cập nhật từ FirebaseUser trong OrderRepository
                createdAt,
                "Đang xử lý",
                payment,
                addressDetail,
                total,
                orderItems
        );

        isLoading.setValue(true);
        orderRepository.placeOrderWithBatch(newOrder, items, new OrderRepository.OrderCallback() {
            @Override
            public void onSuccess(Order order) {
                isLoading.setValue(false);
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

    @Override
    protected void onCleared() {
        super.onCleared();
        if (cartListenerRegistration != null) {
            cartListenerRegistration.remove();
            cartListenerRegistration = null;
        }
        if (orderListenerRegistration != null) {
            orderListenerRegistration.remove();
            orderListenerRegistration = null;
        }
    }
}
