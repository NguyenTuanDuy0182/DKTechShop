package com.ecommerce.dktechshop.ui.cart;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ecommerce.dktechshop.MainActivity;
import com.ecommerce.dktechshop.R;
import com.ecommerce.dktechshop.data.model.CartItem;
import com.ecommerce.dktechshop.data.model.Order;
import com.ecommerce.dktechshop.data.repository.MockRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class CartFragment extends Fragment implements MockRepository.OnDataChangeListener {

    private TextView tvCartTitle;
    private TextView tvUserInfo;
    private TextView tvAddressDetail;
    private CartAdapter adapter;
    private NestedScrollView scrollContent;
    private LinearLayout layoutEmpty;
    private LinearLayout layoutCheckoutBar;
    private TextView tvTotalPrice;

    private String selectedPayment = "COD";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cart, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvCartTitle = view.findViewById(R.id.tv_cart_title);
        MaterialCardView cardAddress = view.findViewById(R.id.card_shipping_address);
        tvUserInfo = view.findViewById(R.id.tv_cart_user_info);
        tvAddressDetail = view.findViewById(R.id.tv_cart_address_detail);
        RecyclerView rvCart = view.findViewById(R.id.rv_cart_items);

        scrollContent = view.findViewById(R.id.scroll_cart_content);
        layoutEmpty = view.findViewById(R.id.layout_cart_empty);
        layoutCheckoutBar = view.findViewById(R.id.layout_checkout_bar);
        tvTotalPrice = view.findViewById(R.id.tv_cart_total_price);
        MaterialButton btnCheckout = view.findViewById(R.id.btn_cart_checkout);
        MaterialButton btnShopNow = view.findViewById(R.id.btn_cart_shop_now);

        RadioButton rbCod = view.findViewById(R.id.rb_pay_cod);
        RadioButton rbVnpay = view.findViewById(R.id.rb_pay_vnpay);
        RadioButton rbMomo = view.findViewById(R.id.rb_pay_momo);

        cardAddress.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToTab(R.id.nav_profile);
            }
        });

        adapter = new CartAdapter((item, delta) -> MockRepository.getInstance().updateCartItemQty(item.getItemId(), delta));

        rvCart.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvCart.setAdapter(adapter);

        rbCod.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) selectedPayment = "COD";
        });
        rbVnpay.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) selectedPayment = "VNPay QR";
        });
        rbMomo.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) selectedPayment = "Ví MoMo";
        });

        btnShopNow.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToTab(R.id.nav_home);
            }
        });

        btnCheckout.setOnClickListener(v -> {
            Order order = MockRepository.getInstance().createOrder(selectedPayment);
            if (order != null) {
                Toast.makeText(requireContext(), "Đặt hàng thành công (" + order.getOrderId() + ")!", Toast.LENGTH_SHORT).show();
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).navigateToTab(R.id.nav_orders);
                }
            }
        });

        refreshCartUI();
        MockRepository.getInstance().addListener(this);
    }

    private void refreshCartUI() {
        List<CartItem> items = MockRepository.getInstance().getCartItems();
        int count = MockRepository.getInstance().getCartCount();
        long total = MockRepository.getInstance().getCartTotal();

        tvCartTitle.setText("Giỏ hàng (" + count + ")");
        tvUserInfo.setText(MockRepository.getInstance().getCurrentUser().getName() + " • " + MockRepository.getInstance().getCurrentUser().getPhone());
        tvAddressDetail.setText(MockRepository.getInstance().getDefaultAddressDetail());
        tvTotalPrice.setText(MockRepository.formatVND(total));

        adapter.setCartItems(items);

        if (items.isEmpty()) {
            scrollContent.setVisibility(View.GONE);
            layoutCheckoutBar.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
        } else {
            scrollContent.setVisibility(View.VISIBLE);
            layoutCheckoutBar.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshCartUI();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        MockRepository.getInstance().removeListener(this);
    }

    @Override
    public void onDataChanged() {
        if (isAdded()) {
            refreshCartUI();
        }
    }
}
