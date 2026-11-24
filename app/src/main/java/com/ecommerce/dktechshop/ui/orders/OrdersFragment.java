package com.ecommerce.dktechshop.ui.orders;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.ecommerce.dktechshop.MainActivity;
import com.ecommerce.dktechshop.R;
import com.ecommerce.dktechshop.data.model.CartItem;
import com.ecommerce.dktechshop.data.model.Order;
import com.ecommerce.dktechshop.data.model.OrderItem;
import com.ecommerce.dktechshop.data.model.Product;
import com.ecommerce.dktechshop.data.repository.CartRepository;
import com.ecommerce.dktechshop.data.repository.MockRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

public class OrdersFragment extends Fragment {

    private OrderViewModel orderViewModel;
    private OrderAdapter adapter;
    private RecyclerView rvOrders;
    private SwipeRefreshLayout swipeRefreshOrders;
    private LinearLayout layoutEmptyOrders;
    private ProgressBar progressOrders;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_orders, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ChipGroup chipGroup = view.findViewById(R.id.chip_group_order_status);
        rvOrders = view.findViewById(R.id.rv_orders);
        swipeRefreshOrders = view.findViewById(R.id.swipe_refresh_orders);
        layoutEmptyOrders = view.findViewById(R.id.layout_empty_orders);
        progressOrders = view.findViewById(R.id.progress_orders);
        MaterialButton btnShopNow = view.findViewById(R.id.btn_shop_now);

        if (swipeRefreshOrders != null) {
            swipeRefreshOrders.setColorSchemeResources(
                    R.color.md_theme_light_primary,
                    R.color.green_accent,
                    R.color.md_theme_light_error
            );
            swipeRefreshOrders.setOnRefreshListener(() -> {
                if (orderViewModel != null) {
                    orderViewModel.startListeningOrders();
                }
            });
        }

        orderViewModel = new ViewModelProvider(this).get(OrderViewModel.class);

        adapter = new OrderAdapter(new OrderAdapter.OnOrderActionListener() {
            @Override
            public void onOrderClick(Order order) {
                // Mở màn hình Chi tiết đơn hàng (OrderDetailActivity)
                Intent intent = new Intent(requireContext(), OrderDetailActivity.class);
                intent.putExtra(OrderDetailActivity.EXTRA_ORDER, order);
                intent.putExtra(OrderDetailActivity.EXTRA_ORDER_ID, order.getOrderId());
                startActivity(intent);
            }

            @Override
            public void onCancelOrder(Order order) {
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Hủy đơn hàng")
                        .setMessage("Bạn có chắc chắn muốn hủy đơn hàng #" + order.getOrderId() + " không?")
                        .setPositiveButton("Hủy đơn", (dialog, which) -> {
                            orderViewModel.updateOrderStatus(order.getOrderId(), "Đã hủy");
                            Toast.makeText(requireContext(), "Đã yêu cầu hủy đơn hàng #" + order.getOrderId(), Toast.LENGTH_SHORT).show();
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
                            orderViewModel.updateOrderStatus(order.getOrderId(), "Đã giao");
                            Toast.makeText(requireContext(), "Cảm ơn bạn! Đơn hàng đã hoàn thành.", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Chưa nhận", null)
                        .show();
            }

            @Override
            public void onReorder(Order order) {
                if (order.getItems() != null && !order.getItems().isEmpty()) {
                    for (OrderItem oi : order.getItems()) {
                        Product p = MockRepository.getInstance().getProductById(oi.getProductId());
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
                    Toast.makeText(requireContext(), "Đã thêm các sản phẩm vào giỏ hàng!", Toast.LENGTH_SHORT).show();
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

        // Nút khám phá khi danh sách trống
        if (btnShopNow != null) {
            btnShopNow.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).navigateToTab(R.id.nav_home);
                }
            });
        }

        // Quan sát dữ liệu đơn hàng thật từ Firestore qua OrderViewModel
        orderViewModel.getFilteredOrders().observe(getViewLifecycleOwner(), orders -> {
            if (swipeRefreshOrders != null) {
                swipeRefreshOrders.setRefreshing(false);
            }
            adapter.setOrders(orders);
            boolean isEmpty = (orders == null || orders.isEmpty());
            rvOrders.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            layoutEmptyOrders.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        });

        orderViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            boolean loading = Boolean.TRUE.equals(isLoading);
            if (progressOrders != null) {
                progressOrders.setVisibility(loading && (swipeRefreshOrders == null || !swipeRefreshOrders.isRefreshing()) ? View.VISIBLE : View.GONE);
            }
            if (!loading && swipeRefreshOrders != null) {
                swipeRefreshOrders.setRefreshing(false);
            }
        });

        orderViewModel.getActionSuccess().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });

        // Xử lý bộ lọc trạng thái (Chips)
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chip_order_processing)) {
                orderViewModel.setStatusFilter("Đang xử lý");
            } else if (checkedIds.contains(R.id.chip_order_delivering)) {
                orderViewModel.setStatusFilter("Đang giao");
            } else if (checkedIds.contains(R.id.chip_order_completed)) {
                orderViewModel.setStatusFilter("Đã giao");
            } else if (checkedIds.contains(R.id.chip_order_cancelled)) {
                orderViewModel.setStatusFilter("Đã hủy");
            } else {
                orderViewModel.setStatusFilter("all");
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (orderViewModel != null) {
            orderViewModel.startListeningOrders();
        }
    }
}
