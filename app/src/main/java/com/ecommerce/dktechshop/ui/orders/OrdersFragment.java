package com.ecommerce.dktechshop.ui.orders;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ecommerce.dktechshop.MainActivity;
import com.ecommerce.dktechshop.R;
import com.ecommerce.dktechshop.data.model.CartItem;
import com.ecommerce.dktechshop.data.model.Order;
import com.ecommerce.dktechshop.data.model.OrderItem;
import com.ecommerce.dktechshop.data.repository.CartRepository;
import com.ecommerce.dktechshop.data.repository.MockRepository;
import com.ecommerce.dktechshop.data.repository.OrderRepository;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class OrdersFragment extends Fragment implements MockRepository.OnDataChangeListener {

    private OrderAdapter adapter;
    private String currentStatusFilter = "all";
    private ListenerRegistration orderListenerRegistration;
    private List<Order> firestoreOrders = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_orders, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ChipGroup chipGroup = view.findViewById(R.id.chip_group_order_status);
        RecyclerView rvOrders = view.findViewById(R.id.rv_orders);

        adapter = new OrderAdapter(new OrderAdapter.OnOrderActionListener() {
            @Override
            public void onCancelOrder(Order order) {
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Hủy đơn hàng")
                        .setMessage("Bạn có chắc chắn muốn hủy đơn hàng #" + order.getOrderId() + " không?")
                        .setPositiveButton("Hủy đơn", (dialog, which) -> {
                            OrderRepository.getInstance().updateOrderStatus(order.getOrderId(), "Đã hủy", new OrderRepository.ActionCallback() {
                                @Override
                                public void onSuccess() {
                                    Toast.makeText(requireContext(), "Đã hủy đơn hàng #" + order.getOrderId(), Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onFailure(String error) {
                                    MockRepository.getInstance().updateOrderStatus(order.getOrderId(), "Đã hủy");
                                    Toast.makeText(requireContext(), "Đã hủy đơn hàng #" + order.getOrderId(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        })
                        .setNegativeButton("Đóng", null)
                        .show();
            }

            @Override
            public void onConfirmReceived(Order order) {
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Xác nhận đã nhận hàng")
                        .setMessage("Bạn xác nhận đã nhận đầy đủ kiện hàng cho đơn #" + order.getOrderId() + "?")
                        .setPositiveButton("Đã nhận hàng", (dialog, which) -> {
                            OrderRepository.getInstance().updateOrderStatus(order.getOrderId(), "Hoàn thành", new OrderRepository.ActionCallback() {
                                @Override
                                public void onSuccess() {
                                    Toast.makeText(requireContext(), "Cảm ơn bạn! Đơn hàng đã hoàn thành.", Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onFailure(String error) {
                                    MockRepository.getInstance().updateOrderStatus(order.getOrderId(), "Hoàn thành");
                                    Toast.makeText(requireContext(), "Cảm ơn bạn! Đơn hàng đã hoàn thành.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        })
                        .setNegativeButton("Chưa nhận", null)
                        .show();
            }

            @Override
            public void onReorder(Order order) {
                if (order.getItems() != null && !order.getItems().isEmpty()) {
                    for (OrderItem oi : order.getItems()) {
                        com.ecommerce.dktechshop.data.model.Product p = MockRepository.getInstance().getProductById(oi.getProductId());
                        String thumb = (p != null) ? p.getThumbnailUrl() : "";
                        String category = (p != null) ? p.getCategory() : "";
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
                        CartRepository.getInstance().upsertCartItem(ci, null);
                        if (p != null) {
                            MockRepository.getInstance().addToCart(p, oi.getVariant(), oi.getQuantity());
                        }
                    }
                    Toast.makeText(requireContext(), "Đã thêm sản phẩm vào giỏ hàng!", Toast.LENGTH_SHORT).show();
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).navigateToTab(R.id.nav_cart);
                    }
                } else {
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).navigateToTab(R.id.nav_home);
                    }
                }
            }
        });

        rvOrders.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvOrders.setAdapter(adapter);

        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chip_order_processing)) {
                currentStatusFilter = "Đang xử lý";
            } else if (checkedIds.contains(R.id.chip_order_delivering)) {
                currentStatusFilter = "Đang giao";
            } else if (checkedIds.contains(R.id.chip_order_completed)) {
                currentStatusFilter = "Hoàn thành";
            } else if (checkedIds.contains(R.id.chip_order_cancelled)) {
                currentStatusFilter = "Đã hủy";
            } else {
                currentStatusFilter = "all";
            }
            updateOrdersList();
        });

        // Lắng nghe đơn hàng Firestore thời gian thực
        orderListenerRegistration = OrderRepository.getInstance().listenOrders(new OrderRepository.OrderListCallback() {
            @Override
            public void onSuccess(List<Order> orders) {
                if (orders != null && !orders.isEmpty()) {
                    firestoreOrders = orders;
                    updateOrdersList();
                }
            }

            @Override
            public void onFailure(String error) {
                // Sử dụng MockRepository làm fallback
            }
        });

        updateOrdersList();
        MockRepository.getInstance().addListener(this);
    }

    private void updateOrdersList() {
        List<Order> source = (!firestoreOrders.isEmpty())
                ? firestoreOrders
                : MockRepository.getInstance().getOrders(currentStatusFilter);

        if (!firestoreOrders.isEmpty()) {
            if ("all".equalsIgnoreCase(currentStatusFilter)) {
                adapter.setOrders(source);
            } else {
                List<Order> filtered = new ArrayList<>();
                for (Order o : source) {
                    if (currentStatusFilter.equalsIgnoreCase(o.getStatus())) {
                        filtered.add(o);
                    }
                }
                adapter.setOrders(filtered);
            }
        } else {
            adapter.setOrders(source);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateOrdersList();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (orderListenerRegistration != null) {
            orderListenerRegistration.remove();
            orderListenerRegistration = null;
        }
        MockRepository.getInstance().removeListener(this);
    }

    @Override
    public void onDataChanged() {
        if (isAdded()) {
            updateOrdersList();
        }
    }
}
