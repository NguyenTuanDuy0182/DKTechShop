package com.ecommerce.dktechshop.ui.orders;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.ecommerce.dktechshop.data.model.Order;
import com.ecommerce.dktechshop.data.repository.OrderRepository;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class OrderViewModel extends ViewModel {

    private final OrderRepository orderRepository;
    private final MutableLiveData<List<Order>> orders = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Order>> filteredOrders = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> currentStatusFilter = new MutableLiveData<>("all");
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> actionSuccess = new MutableLiveData<>();

    private ListenerRegistration ordersListenerRegistration;

    public OrderViewModel() {
        this(OrderRepository.getInstance());
    }

    public OrderViewModel(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
        startListeningOrders();
    }

    public LiveData<List<Order>> getOrders() {
        return orders;
    }

    public LiveData<List<Order>> getFilteredOrders() {
        return filteredOrders;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<String> getActionSuccess() {
        return actionSuccess;
    }

    public void setStatusFilter(String filter) {
        currentStatusFilter.setValue(filter);
        applyFilter();
    }

    private void applyFilter() {
        List<Order> current = orders.getValue();
        if (current == null) {
            filteredOrders.setValue(new ArrayList<>());
            return;
        }

        String filter = currentStatusFilter.getValue();
        if (filter == null || "all".equalsIgnoreCase(filter)) {
            filteredOrders.setValue(new ArrayList<>(current));
            return;
        }

        List<Order> result = new ArrayList<>();
        for (Order o : current) {
            if (o.getStatus() == null) continue;
            // Hỗ trợ cả "Đã giao" và "Hoàn thành" cho trạng thái giao hàng thành công
            if ("Đã giao".equalsIgnoreCase(filter) || "Hoàn thành".equalsIgnoreCase(filter)) {
                if ("Đã giao".equalsIgnoreCase(o.getStatus()) || "Hoàn thành".equalsIgnoreCase(o.getStatus())) {
                    result.add(o);
                }
            } else if (filter.equalsIgnoreCase(o.getStatus())) {
                result.add(o);
            }
        }
        filteredOrders.setValue(result);
    }

    /**
     * Bắt đầu lắng nghe đơn hàng người dùng theo thời gian thực qua listenUserOrders của OrderRepository
     */
    public void startListeningOrders() {
        if (ordersListenerRegistration != null) {
            ordersListenerRegistration.remove();
        }

        isLoading.setValue(true);

        ordersListenerRegistration = orderRepository.listenUserOrders(new OrderRepository.OrderListCallback() {
            @Override
            public void onSuccess(List<Order> list) {
                isLoading.setValue(false);
                orders.setValue(list != null ? list : new ArrayList<>());
                applyFilter();
            }

            @Override
            public void onFailure(String error) {
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }

    public void updateOrderStatus(String orderId, String newStatus) {
        isLoading.setValue(true);
        orderRepository.updateOrderStatus(orderId, newStatus, new OrderRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                isLoading.setValue(false);
                actionSuccess.setValue("Cập nhật đơn hàng thành công!");
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
        if (ordersListenerRegistration != null) {
            ordersListenerRegistration.remove();
            ordersListenerRegistration = null;
        }
    }
}
