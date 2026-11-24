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
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.ecommerce.dktechshop.MainActivity;
import com.ecommerce.dktechshop.R;
import com.ecommerce.dktechshop.data.model.Address;
import com.ecommerce.dktechshop.data.model.CartItem;
import com.ecommerce.dktechshop.data.repository.MockRepository;
import com.ecommerce.dktechshop.ui.auth.AuthViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class CartFragment extends Fragment {

    private TextView tvCartTitle;
    private TextView tvUserInfo;
    private TextView tvAddressDetail;
    private CartAdapter adapter;
    private SwipeRefreshLayout swipeRefreshCart;
    private NestedScrollView scrollContent;
    private LinearLayout layoutEmpty;
    private LinearLayout layoutCheckoutBar;
    private TextView tvTotalPrice;
    private MaterialButton btnCheckout;

    private MaterialCardView cardPayCod;
    private MaterialCardView cardPayVnpay;
    private MaterialCardView cardPayMomo;
    private RadioButton rbCod;
    private RadioButton rbVnpay;
    private RadioButton rbMomo;

    private String selectedPayment = "COD";
    private CartViewModel cartViewModel;
    private AuthViewModel authViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cart, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        cartViewModel = new ViewModelProvider(requireActivity()).get(CartViewModel.class);
        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        tvCartTitle = view.findViewById(R.id.tv_cart_title);
        MaterialCardView cardAddress = view.findViewById(R.id.card_shipping_address);
        tvUserInfo = view.findViewById(R.id.tv_cart_user_info);
        tvAddressDetail = view.findViewById(R.id.tv_cart_address_detail);
        RecyclerView rvCart = view.findViewById(R.id.rv_cart_items);

        swipeRefreshCart = view.findViewById(R.id.swipe_refresh_cart);
        if (swipeRefreshCart != null) {
            swipeRefreshCart.setColorSchemeResources(R.color.md_theme_light_primary);
            swipeRefreshCart.setOnRefreshListener(() -> {
                cartViewModel.startListeningCart();
                authViewModel.loadCurrentUser();
                authViewModel.loadAddresses();
                updateAddressUI();
                swipeRefreshCart.postDelayed(() -> {
                    if (swipeRefreshCart != null) {
                        swipeRefreshCart.setRefreshing(false);
                    }
                }, 800);
            });
        }

        scrollContent = view.findViewById(R.id.scroll_cart_content);
        layoutEmpty = view.findViewById(R.id.layout_cart_empty);
        layoutCheckoutBar = view.findViewById(R.id.layout_checkout_bar);
        tvTotalPrice = view.findViewById(R.id.tv_cart_total_price);
        btnCheckout = view.findViewById(R.id.btn_cart_checkout);
        MaterialButton btnShopNow = view.findViewById(R.id.btn_cart_shop_now);

        cardPayCod = view.findViewById(R.id.card_pay_cod);
        cardPayVnpay = view.findViewById(R.id.card_pay_vnpay);
        cardPayMomo = view.findViewById(R.id.card_pay_momo);

        rbCod = view.findViewById(R.id.rb_pay_cod);
        rbVnpay = view.findViewById(R.id.rb_pay_vnpay);
        rbMomo = view.findViewById(R.id.rb_pay_momo);

        cardAddress.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToTab(R.id.nav_profile);
            }
        });

        adapter = new CartAdapter((item, delta) -> cartViewModel.updateQuantity(item.getItemId(), delta));

        rvCart.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvCart.setAdapter(adapter);

        // Click listeners cho các phương thức thanh toán (chọn 1 duy nhất)
        cardPayCod.setOnClickListener(v -> selectPaymentMethod("COD"));
        rbCod.setOnClickListener(v -> selectPaymentMethod("COD"));

        cardPayVnpay.setOnClickListener(v -> selectPaymentMethod("VNPay QR"));
        rbVnpay.setOnClickListener(v -> selectPaymentMethod("VNPay QR"));

        cardPayMomo.setOnClickListener(v -> selectPaymentMethod("Ví MoMo"));
        rbMomo.setOnClickListener(v -> selectPaymentMethod("Ví MoMo"));

        selectPaymentMethod("COD");

        btnShopNow.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToTab(R.id.nav_home);
            }
        });

        // Đặt hàng an toàn qua CartViewModel.checkout
        btnCheckout.setOnClickListener(v -> {
            List<CartItem> items = cartViewModel.getCartItems().getValue();
            if (items == null || items.isEmpty()) {
                Toast.makeText(requireContext(), "Giỏ hàng đang trống", Toast.LENGTH_SHORT).show();
                return;
            }

            ShippingInfo shipping = resolveShippingInfo();
            cartViewModel.checkout(shipping.address, selectedPayment, shipping.name, shipping.phone);
        });

        observeViewModel();
        updateAddressUI();
    }

    private static class ShippingInfo {
        final Address address;
        final String name;
        final String phone;

        ShippingInfo(Address address, String name, String phone) {
            this.address = address;
            this.name = name;
            this.phone = phone;
        }
    }

    private ShippingInfo resolveShippingInfo() {
        Address defaultAddress = null;
        List<Address> addresses = authViewModel.getAddresses().getValue();
        if (addresses != null) {
            for (Address a : addresses) {
                if (a.isDefault()) {
                    defaultAddress = a;
                    break;
                }
            }
            if (defaultAddress == null && !addresses.isEmpty()) {
                defaultAddress = addresses.get(0);
            }
        }
        if (defaultAddress == null) {
            defaultAddress = new Address("", "Mặc định", MockRepository.getInstance().getDefaultAddressDetail(), true);
        }

        String name = "";
        String phone = "";
        com.ecommerce.dktechshop.data.model.User u = authViewModel.getCurrentUser().getValue();
        if (u != null) {
            name = u.getName();
            phone = u.getPhone();
        }
        if (name == null || name.trim().isEmpty()) {
            com.google.firebase.auth.FirebaseUser fbUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            if (fbUser != null && fbUser.getDisplayName() != null && !fbUser.getDisplayName().trim().isEmpty()) {
                name = fbUser.getDisplayName();
            } else {
                name = MockRepository.getInstance().getCurrentUser().getName();
            }
        }
        if (defaultAddress.getPhone() != null && !defaultAddress.getPhone().trim().isEmpty()) {
            phone = defaultAddress.getPhone().trim();
        } else if (phone == null || phone.trim().isEmpty()) {
            phone = MockRepository.getInstance().getCurrentUser().getPhone();
        }

        return new ShippingInfo(defaultAddress, name != null ? name : "", phone != null ? phone : "");
    }

    /**
     * Đảm bảo chỉ 1 phương thức thanh toán được chọn tại một thời điểm
     */
    private void selectPaymentMethod(String method) {
        selectedPayment = method;

        boolean isCod = "COD".equals(method);
        boolean isVnpay = "VNPay QR".equals(method);
        boolean isMomo = "Ví MoMo".equals(method);

        if (rbCod != null) rbCod.setChecked(isCod);
        if (rbVnpay != null) rbVnpay.setChecked(isVnpay);
        if (rbMomo != null) rbMomo.setChecked(isMomo);

        if (!isAdded()) return;

        int colorPrimary = ContextCompat.getColor(requireContext(), R.color.md_theme_light_primary);
        int colorOutline = ContextCompat.getColor(requireContext(), R.color.md_theme_light_outlineVariant);
        int bgSelected = ContextCompat.getColor(requireContext(), R.color.md_theme_light_secondaryContainer);
        int bgDefault = ContextCompat.getColor(requireContext(), R.color.md_theme_light_surfaceContainerLowest);

        if (cardPayCod != null) {
            cardPayCod.setStrokeColor(isCod ? colorPrimary : colorOutline);
            cardPayCod.setCardBackgroundColor(isCod ? bgSelected : bgDefault);
        }
        if (cardPayVnpay != null) {
            cardPayVnpay.setStrokeColor(isVnpay ? colorPrimary : colorOutline);
            cardPayVnpay.setCardBackgroundColor(isVnpay ? bgSelected : bgDefault);
        }
        if (cardPayMomo != null) {
            cardPayMomo.setStrokeColor(isMomo ? colorPrimary : colorOutline);
            cardPayMomo.setCardBackgroundColor(isMomo ? bgSelected : bgDefault);
        }
    }

    private void observeViewModel() {
        // Lắng nghe danh sách cartItems từ CartViewModel (Realtime Firestore)
        cartViewModel.getCartItems().observe(getViewLifecycleOwner(), items -> {
            if (swipeRefreshCart != null) {
                swipeRefreshCart.setRefreshing(false);
            }
            adapter.setCartItems(items);

            int count = 0;
            if (items != null) {
                for (CartItem item : items) {
                    count += item.getQuantity();
                }
            }
            tvCartTitle.setText("Giỏ hàng (" + count + ")");

            if (items == null || items.isEmpty()) {
                scrollContent.setVisibility(View.GONE);
                layoutCheckoutBar.setVisibility(View.GONE);
                layoutEmpty.setVisibility(View.VISIBLE);
            } else {
                scrollContent.setVisibility(View.VISIBLE);
                layoutCheckoutBar.setVisibility(View.VISIBLE);
                layoutEmpty.setVisibility(View.GONE);
            }
        });

        // Lắng nghe tổng tiền từ CartViewModel
        cartViewModel.getTotalAmount().observe(getViewLifecycleOwner(), total -> {
            long totalVal = total != null ? total.longValue() : 0L;
            tvTotalPrice.setText(MockRepository.formatVND(totalVal));
        });

        // Lắng nghe trạng thái đặt hàng thành công
        cartViewModel.getCheckoutSuccessOrder().observe(getViewLifecycleOwner(), order -> {
            if (order != null) {
                Toast.makeText(requireContext(), "Đặt hàng thành công (" + order.getOrderId() + ")!", Toast.LENGTH_SHORT).show();
                cartViewModel.clearCheckoutSuccess();
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).navigateToTab(R.id.nav_orders);
                }
            }
        });

        // Lắng nghe trạng thái loading khi đặt hàng
        cartViewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            boolean isLoading = Boolean.TRUE.equals(loading);
            if (btnCheckout != null) {
                btnCheckout.setEnabled(!isLoading);
                btnCheckout.setText(isLoading ? "Đang xử lý..." : "Đặt hàng");
            }
        });

        // Lắng nghe thông báo lỗi
        cartViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });

        // Lắng nghe thay đổi User profile & addresses
        authViewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            updateAddressUI();
        });
        authViewModel.getAddresses().observe(getViewLifecycleOwner(), addresses -> {
            updateAddressUI();
        });
    }

    private void updateAddressUI() {
        if (tvUserInfo != null && tvAddressDetail != null) {
            ShippingInfo shipping = resolveShippingInfo();
            tvUserInfo.setText(shipping.name + (!shipping.phone.isEmpty() ? " • " + shipping.phone : ""));
            tvAddressDetail.setText(shipping.address.getDetail() != null && !shipping.address.getDetail().isEmpty()
                    ? shipping.address.getDetail()
                    : MockRepository.getInstance().getDefaultAddressDetail());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateAddressUI();
    }
}
