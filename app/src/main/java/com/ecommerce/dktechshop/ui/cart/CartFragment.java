package com.ecommerce.dktechshop.ui.cart;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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
import com.ecommerce.dktechshop.data.model.User;
import com.ecommerce.dktechshop.data.repository.AuthRepository;
import com.ecommerce.dktechshop.ui.auth.AuthViewModel;
import com.ecommerce.dktechshop.utils.CurrencyUtils;
import com.ecommerce.dktechshop.utils.NetworkMonitor;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;
import java.util.Set;

public class CartFragment extends Fragment {

    private TextView tvCartTitle;
    private TextView tvUserInfo;
    private TextView tvAddressDetail;
    private MaterialCheckBox cbSelectAll;
    private TextView tvSelectedCount;
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
    private Address selectedShippingAddress;
    private boolean selectNewestAddressOnUpdate = false;
    private int previousAddressCount = 0;

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
        cbSelectAll = view.findViewById(R.id.cb_cart_select_all);
        tvSelectedCount = view.findViewById(R.id.tv_cart_selected_count);
        RecyclerView rvCart = view.findViewById(R.id.rv_cart_items);

        swipeRefreshCart = view.findViewById(R.id.swipe_refresh_cart);
        if (swipeRefreshCart != null) {
            swipeRefreshCart.setColorSchemeResources(R.color.md_theme_light_primary);
            swipeRefreshCart.setOnRefreshListener(() -> {
                if (!NetworkMonitor.isConnected(requireContext())) {
                    swipeRefreshCart.setRefreshing(false);
                    Toast.makeText(requireContext(), NetworkMonitor.NO_INTERNET_MESSAGE, Toast.LENGTH_SHORT).show();
                    return;
                }
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

        // Đổi nhanh địa chỉ ngay trong giỏ hàng bằng BottomSheet
        cardAddress.setOnClickListener(v -> showAddressPickerBottomSheet());

        adapter = new CartAdapter(new CartAdapter.OnCartItemActionListener() {
            @Override
            public void onQuantityChanged(CartItem item, int delta) {
                if (!NetworkMonitor.isConnected(requireContext())) {
                    Toast.makeText(requireContext(), NetworkMonitor.NO_INTERNET_MESSAGE, Toast.LENGTH_SHORT).show();
                    return;
                }
                cartViewModel.updateQuantity(item.getItemId(), delta);
            }

            @Override
            public void onSelectionChanged(CartItem item, boolean isSelected) {
                cartViewModel.setItemSelected(item.getItemId(), isSelected);
            }
        });

        rvCart.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvCart.setAdapter(adapter);

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

        btnCheckout.setOnClickListener(v -> {
            if (!NetworkMonitor.isConnected(requireContext())) {
                Toast.makeText(requireContext(), NetworkMonitor.NO_INTERNET_MESSAGE, Toast.LENGTH_SHORT).show();
                return;
            }

            List<CartItem> selectedItems = cartViewModel.getSelectedCartItems();
            if (selectedItems.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng chọn ít nhất 1 sản phẩm để thanh toán", Toast.LENGTH_SHORT).show();
                return;
            }

            ShippingInfo shipping = resolveShippingInfo();
            if (shipping.address == null || shipping.address.getDetail() == null || shipping.address.getDetail().trim().isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng thêm địa chỉ và số điện thoại nhận hàng", Toast.LENGTH_SHORT).show();
                showAddressDialog(null);
                return;
            }
            if (!CartViewModel.isValidPhone(shipping.phone)) {
                Toast.makeText(requireContext(), "Vui lòng cập nhật số điện thoại nhận hàng trước khi đặt hàng", Toast.LENGTH_SHORT).show();
                showAddressDialog(shipping.address);
                return;
            }

            if ("COD".equalsIgnoreCase(selectedPayment)) {
                cartViewModel.checkout(shipping.address, selectedPayment, shipping.name, shipping.phone);
            } else {
                long totalAmount = CartViewModel.calculateSelectedTotal(
                        selectedItems, cartViewModel.getSelectedItemIds().getValue());
                String orderId = CartViewModel.generateOrderId();
                PaymentDemoDialog.show(requireContext(), selectedPayment, orderId, totalAmount, shipping.phone, () -> {
                    if (isAdded()) {
                        cartViewModel.checkout(shipping.address, selectedPayment, shipping.name, shipping.phone, orderId);
                    }
                });
            }
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

    private Address resolveActiveAddress() {
        List<Address> addresses = authViewModel.getAddresses().getValue();
        if (addresses != null && !addresses.isEmpty()) {
            // Giữ lại địa chỉ người dùng đã chọn nhanh nếu vẫn còn trong danh sách
            if (selectedShippingAddress != null && selectedShippingAddress.getId() != null) {
                for (Address a : addresses) {
                    if (selectedShippingAddress.getId().equals(a.getId())) {
                        selectedShippingAddress = a;
                        return a;
                    }
                }
            }
            for (Address a : addresses) {
                if (a.isDefault()) {
                    selectedShippingAddress = a;
                    return a;
                }
            }
            selectedShippingAddress = addresses.get(0);
            return selectedShippingAddress;
        }

        // Fallback về trường address trên User document nếu chưa tạo subcollection addresses
        User u = authViewModel.getCurrentUser().getValue();
        if (u == null) u = AuthRepository.getInstance().getCachedUser();
        if (u != null && u.getAddress() != null && !u.getAddress().trim().isEmpty()) {
            return new Address("", "Mặc định", u.getAddress().trim(), u.getPhone(), true);
        }
        return null;
    }

    private ShippingInfo resolveShippingInfo() {
        Address activeAddress = resolveActiveAddress();

        String name = "";
        String phone = "";
        User u = authViewModel.getCurrentUser().getValue();
        if (u == null) u = AuthRepository.getInstance().getCachedUser();
        if (u != null) {
            name = u.getName();
            phone = u.getPhone();
        }
        if (name == null || name.trim().isEmpty()) {
            FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
            if (fbUser != null && fbUser.getDisplayName() != null && !fbUser.getDisplayName().trim().isEmpty()) {
                name = fbUser.getDisplayName();
            } else {
                name = "Khách hàng";
            }
        }
        if (activeAddress != null && activeAddress.getPhone() != null && !activeAddress.getPhone().trim().isEmpty()) {
            phone = activeAddress.getPhone().trim();
        }

        return new ShippingInfo(activeAddress, name.trim(), phone != null ? phone.trim() : "");
    }

    private void showAddressPickerBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext(), R.style.Theme_DKTechShop_BottomSheetDialog);
        View sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_address_picker, null);
        dialog.setContentView(sheetView);

        MaterialButton btnClose = sheetView.findViewById(R.id.btn_address_picker_close);
        TextView tvEmpty = sheetView.findViewById(R.id.tv_picker_empty);
        LinearLayout layoutList = sheetView.findViewById(R.id.layout_picker_address_list);
        MaterialButton btnAddNew = sheetView.findViewById(R.id.btn_picker_add_address);

        btnClose.setOnClickListener(v -> dialog.dismiss());

        List<Address> addresses = authViewModel.getAddresses().getValue();
        Address active = resolveActiveAddress();

        if (addresses == null || addresses.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            float density = getResources().getDisplayMetrics().density;
            int padH = (int) (14 * density);
            int padV = (int) (12 * density);
            int marginB = (int) (8 * density);

            for (Address addr : addresses) {
                boolean isCurrent = active != null
                        && ((active.getId() != null && !active.getId().isEmpty() && active.getId().equals(addr.getId()))
                        || (active.getDetail() != null && active.getDetail().equals(addr.getDetail())));

                MaterialCardView card = new MaterialCardView(requireContext());
                LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                cardLp.setMargins(0, 0, 0, marginB);
                card.setLayoutParams(cardLp);
                card.setRadius(14 * density);
                card.setCardElevation(0f);
                card.setStrokeWidth((int) (1 * density));
                card.setStrokeColor(ContextCompat.getColor(requireContext(),
                        isCurrent ? R.color.md_theme_light_primary : R.color.md_theme_light_outlineVariant));
                card.setCardBackgroundColor(ContextCompat.getColor(requireContext(),
                        isCurrent ? R.color.md_theme_light_secondaryContainer : R.color.md_theme_light_surfaceContainerLowest));
                card.setClickable(true);
                card.setFocusable(true);

                LinearLayout row = new LinearLayout(requireContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(padH, padV, padH, padV);

                LinearLayout textCol = new LinearLayout(requireContext());
                textCol.setOrientation(LinearLayout.VERTICAL);
                textCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                LinearLayout titleRow = new LinearLayout(requireContext());
                titleRow.setOrientation(LinearLayout.HORIZONTAL);
                titleRow.setGravity(Gravity.CENTER_VERTICAL);

                TextView tvLabel = new TextView(requireContext());
                tvLabel.setText(addr.getLabel() != null && !addr.getLabel().isEmpty() ? addr.getLabel() : "Địa chỉ");
                tvLabel.setTextSize(13f);
                tvLabel.setTypeface(null, android.graphics.Typeface.BOLD);
                tvLabel.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_light_onSurface));
                titleRow.addView(tvLabel);

                if (addr.isDefault()) {
                    TextView tvBadge = new TextView(requireContext());
                    tvBadge.setText("Mặc định");
                    tvBadge.setTextSize(10f);
                    tvBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_light_onPrimary));
                    tvBadge.setBackgroundResource(R.drawable.bg_banner_gradient);
                    tvBadge.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.md_theme_light_primary));
                    tvBadge.setPadding((int) (8 * density), (int) (2 * density), (int) (8 * density), (int) (2 * density));
                    LinearLayout.LayoutParams lpBadge = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    lpBadge.setMargins((int) (8 * density), 0, 0, 0);
                    tvBadge.setLayoutParams(lpBadge);
                    titleRow.addView(tvBadge);
                }
                textCol.addView(titleRow);

                TextView tvDetail = new TextView(requireContext());
                tvDetail.setText(addr.getDetail());
                tvDetail.setTextSize(12f);
                tvDetail.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_light_onSurfaceVariant));
                tvDetail.setPadding(0, (int) (3 * density), 0, 0);
                textCol.addView(tvDetail);

                if (addr.getPhone() != null && !addr.getPhone().trim().isEmpty()) {
                    TextView tvPhone = new TextView(requireContext());
                    tvPhone.setText("SĐT: " + addr.getPhone().trim());
                    tvPhone.setTextSize(11f);
                    tvPhone.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_light_onSurfaceVariant));
                    tvPhone.setPadding(0, (int) (2 * density), 0, 0);
                    textCol.addView(tvPhone);
                }

                RadioButton rb = new RadioButton(requireContext());
                rb.setChecked(isCurrent);
                rb.setClickable(false);
                rb.setFocusable(false);

                row.addView(textCol);
                row.addView(rb);
                card.addView(row);

                card.setOnClickListener(v -> {
                    selectedShippingAddress = addr;
                    updateAddressUI();
                    dialog.dismiss();
                    Toast.makeText(requireContext(), "Đã chọn địa chỉ: " + addr.getLabel(), Toast.LENGTH_SHORT).show();
                });

                layoutList.addView(card);
            }
        }

        btnAddNew.setOnClickListener(v -> {
            dialog.dismiss();
            showAddressDialog(null);
        });

        dialog.show();
    }

    private void showAddressDialog(@Nullable Address addrToEdit) {
        boolean isEdit = addrToEdit != null && addrToEdit.getId() != null && !addrToEdit.getId().trim().isEmpty();
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_address, null);
        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_address_title);
        TextView tvSubtitle = dialogView.findViewById(R.id.tv_dialog_address_subtitle);
        EditText etLabel = dialogView.findViewById(R.id.et_dialog_address_label);
        EditText etPhone = dialogView.findViewById(R.id.et_dialog_address_phone);
        EditText etDetail = dialogView.findViewById(R.id.et_dialog_address_detail);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_dialog_cancel);
        MaterialButton btnSave = dialogView.findViewById(R.id.btn_dialog_save);

        if (addrToEdit != null) {
            tvTitle.setText("Cập nhật thông tin giao hàng");
            tvSubtitle.setText("Vui lòng bổ sung đầy đủ số điện thoại và địa chỉ");
            btnSave.setText("Lưu thông tin");
            etLabel.setText(addrToEdit.getLabel());
            etPhone.setText(addrToEdit.getPhone());
            etDetail.setText(addrToEdit.getDetail());
            if (addrToEdit.getPhone() == null || addrToEdit.getPhone().trim().isEmpty()) {
                etPhone.requestFocus();
            }
        } else {
            tvTitle.setText("Thêm địa chỉ giao hàng");
            tvSubtitle.setText("Nhập thông tin giao hàng mới của bạn");
            btnSave.setText("Lưu địa chỉ");
            User u = authViewModel.getCurrentUser().getValue();
            if (u != null && u.getPhone() != null) {
                etPhone.setText(u.getPhone().trim());
            }
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            if (!NetworkMonitor.isConnected(requireContext())) {
                Toast.makeText(requireContext(), NetworkMonitor.NO_INTERNET_MESSAGE, Toast.LENGTH_SHORT).show();
                return;
            }
            String label = etLabel.getText() != null ? etLabel.getText().toString().trim() : "";
            String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
            String detail = etDetail.getText() != null ? etDetail.getText().toString().trim() : "";

            if (!CartViewModel.isValidPhone(phone)) {
                Toast.makeText(requireContext(), "Vui lòng nhập số điện thoại hợp lệ (10-11 chữ số)", Toast.LENGTH_SHORT).show();
                return;
            }
            if (detail.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng nhập địa chỉ chi tiết", Toast.LENGTH_SHORT).show();
                return;
            }
            if (label.isEmpty()) label = isEdit ? "Địa chỉ" : "Địa chỉ mới";

            // Đồng bộ luôn SĐT vào hồ sơ User nếu tài khoản (VD: Google) chưa có SĐT chính
            User u = authViewModel.getCurrentUser().getValue();
            if (u != null && !CartViewModel.isValidPhone(u.getPhone())) {
                String safeName = (u.getName() != null && !u.getName().trim().isEmpty()) ? u.getName().trim() : "Người dùng";
                authViewModel.updateDisplayName(safeName, phone);
            }

            if (isEdit) {
                Address updated = new Address(addrToEdit.getId(), label, detail, phone, addrToEdit.isDefault());
                selectedShippingAddress = updated;
                updateAddressUI();
                authViewModel.updateAddress(updated);
            } else {
                List<Address> current = authViewModel.getAddresses().getValue();
                previousAddressCount = current != null ? current.size() : 0;
                selectNewestAddressOnUpdate = true;
                selectedShippingAddress = new Address("", label, detail, phone, false);
                updateAddressUI();
                authViewModel.addAddress(label, detail, phone);
            }
            dialog.dismiss();
        });

        dialog.show();
    }

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
            updateSelectionHeaderUI();
        });

        cartViewModel.getSelectedItemIds().observe(getViewLifecycleOwner(), selectedIds -> {
            adapter.setSelectedIds(selectedIds);
            updateSelectionHeaderUI();
        });

        cartViewModel.getTotalAmount().observe(getViewLifecycleOwner(), total -> {
            long totalVal = total != null ? total.longValue() : 0L;
            tvTotalPrice.setText(CurrencyUtils.formatVND(totalVal));
        });

        cartViewModel.getSelectedTotalQuantity().observe(getViewLifecycleOwner(), qty -> updateCheckoutButtonState());

        cartViewModel.getCheckoutSuccessOrder().observe(getViewLifecycleOwner(), order -> {
            if (order != null) {
                Toast.makeText(requireContext(), "Đặt hàng thành công (" + order.getOrderId() + ")!", Toast.LENGTH_SHORT).show();
                cartViewModel.clearCheckoutSuccess();
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).navigateToTab(R.id.nav_orders);
                }
            }
        });

        cartViewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> updateCheckoutButtonState());

        cartViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                cartViewModel.clearErrorMessage();
            }
        });

        authViewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> updateAddressUI());
        authViewModel.getAddresses().observe(getViewLifecycleOwner(), addresses -> {
            if (selectNewestAddressOnUpdate && addresses != null && addresses.size() > previousAddressCount) {
                selectNewestAddressOnUpdate = false;
                if (selectedShippingAddress != null && selectedShippingAddress.getDetail() != null) {
                    for (Address a : addresses) {
                        if (selectedShippingAddress.getDetail().equals(a.getDetail())) {
                            selectedShippingAddress = a;
                            break;
                        }
                    }
                }
            }
            updateAddressUI();
        });
    }

    private void updateSelectionHeaderUI() {
        List<CartItem> items = cartViewModel.getCartItems().getValue();
        Set<String> selectedIds = cartViewModel.getSelectedItemIds().getValue();
        int totalItems = items != null ? items.size() : 0;
        int selectedCount = 0;
        if (items != null && selectedIds != null) {
            for (CartItem item : items) {
                if (selectedIds.contains(item.getItemId())) {
                    selectedCount++;
                }
            }
        }

        if (cbSelectAll != null) {
            cbSelectAll.setOnCheckedChangeListener(null);
            cbSelectAll.setChecked(totalItems > 0 && selectedCount == totalItems);
            cbSelectAll.setOnCheckedChangeListener((buttonView, isChecked) -> cartViewModel.selectAll(isChecked));
        }

        if (tvSelectedCount != null) {
            tvSelectedCount.setText("Đã chọn " + selectedCount + "/" + totalItems);
        }

        updateCheckoutButtonState();
    }

    private void updateCheckoutButtonState() {
        if (btnCheckout == null) return;
        boolean isLoading = Boolean.TRUE.equals(cartViewModel.getIsLoading().getValue());
        Integer selQty = cartViewModel.getSelectedTotalQuantity().getValue();
        int qty = selQty != null ? selQty : 0;

        if (isLoading) {
            btnCheckout.setEnabled(false);
            btnCheckout.setText("Đang xử lý...");
        } else {
            btnCheckout.setEnabled(qty > 0);
            btnCheckout.setText(qty > 0 ? "Đặt hàng (" + qty + ")" : "Đặt hàng");
        }
    }

    private void updateAddressUI() {
        if (tvUserInfo != null && tvAddressDetail != null) {
            ShippingInfo shipping = resolveShippingInfo();
            String labelPrefix = (shipping.address != null && shipping.address.getLabel() != null && !shipping.address.getLabel().trim().isEmpty())
                    ? "[" + shipping.address.getLabel().trim() + "] "
                    : "";
            tvUserInfo.setText(shipping.name + (!shipping.phone.isEmpty() ? " • " + shipping.phone : " • Chưa có SĐT"));
            if (shipping.address != null && shipping.address.getDetail() != null && !shipping.address.getDetail().trim().isEmpty()) {
                tvAddressDetail.setText(labelPrefix + shipping.address.getDetail().trim());
            } else {
                tvAddressDetail.setText("Chưa có địa chỉ giao hàng — nhấn để thêm");
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateAddressUI();
    }
}
