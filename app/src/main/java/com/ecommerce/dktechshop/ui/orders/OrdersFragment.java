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
import com.ecommerce.dktechshop.data.model.Order;
import com.ecommerce.dktechshop.data.repository.MockRepository;
import com.google.android.material.chip.ChipGroup;

import java.util.List;

public class OrdersFragment extends Fragment implements MockRepository.OnDataChangeListener {

    private OrderAdapter adapter;
    private String currentStatusFilter = "all";

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

        adapter = new OrderAdapter(order -> {
            if ("Hoàn thành".equalsIgnoreCase(order.getStatus())) {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).navigateToTab(R.id.nav_home);
                }
            } else {
                MockRepository.getInstance().advanceOrderStatus(order.getOrderId());
                Toast.makeText(requireContext(), "Đơn " + order.getOrderId() + " → " + order.getStatus(), Toast.LENGTH_SHORT).show();
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
            } else {
                currentStatusFilter = "all";
            }
            updateOrdersList();
        });

        updateOrdersList();
        MockRepository.getInstance().addListener(this);
    }

    private void updateOrdersList() {
        List<Order> orders = MockRepository.getInstance().getOrders(currentStatusFilter);
        adapter.setOrders(orders);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateOrdersList();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        MockRepository.getInstance().removeListener(this);
    }

    @Override
    public void onDataChanged() {
        if (isAdded()) {
            updateOrdersList();
        }
    }
}
