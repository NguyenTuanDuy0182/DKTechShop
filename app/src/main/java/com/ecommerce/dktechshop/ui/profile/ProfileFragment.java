package com.ecommerce.dktechshop.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.ecommerce.dktechshop.MainActivity;
import com.ecommerce.dktechshop.R;
import com.ecommerce.dktechshop.data.model.Address;
import com.ecommerce.dktechshop.data.model.User;
import com.ecommerce.dktechshop.ui.auth.AuthActivity;
import com.ecommerce.dktechshop.ui.auth.AuthViewModel;
import com.ecommerce.dktechshop.ui.cart.CartViewModel;
import com.ecommerce.dktechshop.utils.NetworkMonitor;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class ProfileFragment extends Fragment {

    private TextView tvName;
    private TextView tvEmail;
    private TextView tvPhone;
    private SwipeRefreshLayout swipeRefreshProfile;

    // Accordions
    private LinearLayout layoutExpandedAddress;
    private LinearLayout layoutExpandedRename;
    private LinearLayout layoutExpandedPassword;
    private ImageView ivArrowAddress;
    private ImageView ivArrowRename;
    private ImageView ivArrowPassword;

    // Address section views
    private TextView tvAddressCount;
    private MaterialButton btnToggleAddAddress;
    private LinearLayout layoutAddressList;

    // Rename section views
    private EditText etEditName;
    private EditText etEditPhone;

    // Password section views
    private LinearLayout menuItemPassword;
    private TextView tvPasswordSubtitle;
    private EditText etCurrentPass;
    private EditText etNewPass;
    private EditText etConfirmPass;
    private MaterialButton btnSavePass;

    private AuthViewModel authViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        tvName = view.findViewById(R.id.tv_profile_name);
        tvEmail = view.findViewById(R.id.tv_profile_email);
        tvPhone = view.findViewById(R.id.tv_profile_phone);
        swipeRefreshProfile = view.findViewById(R.id.swipe_refresh_profile);
        if (swipeRefreshProfile != null) {
            swipeRefreshProfile.setColorSchemeResources(R.color.md_theme_light_primary);
            swipeRefreshProfile.setOnRefreshListener(() -> {
                if (!NetworkMonitor.isConnected(requireContext())) {
                    swipeRefreshProfile.setRefreshing(false);
                    Toast.makeText(requireContext(), NetworkMonitor.NO_INTERNET_MESSAGE, Toast.LENGTH_SHORT).show();
                    return;
                }
                authViewModel.loadCurrentUser();
                authViewModel.loadAddresses();
                swipeRefreshProfile.postDelayed(() -> {
                    if (swipeRefreshProfile != null) {
                        swipeRefreshProfile.setRefreshing(false);
                    }
                }, 800);
            });
        }
        TextView btnEditNameTrigger = view.findViewById(R.id.btn_profile_edit_name_trigger);

        LinearLayout menuItemOrders = view.findViewById(R.id.menu_item_orders);
        LinearLayout menuItemAddress = view.findViewById(R.id.menu_item_address_header);
        LinearLayout menuItemRename = view.findViewById(R.id.menu_item_rename_header);
        menuItemPassword = view.findViewById(R.id.menu_item_password_header);
        tvPasswordSubtitle = view.findViewById(R.id.tv_menu_password_subtitle);

        layoutExpandedAddress = view.findViewById(R.id.layout_expanded_address);
        layoutExpandedRename = view.findViewById(R.id.layout_expanded_rename);
        layoutExpandedPassword = view.findViewById(R.id.layout_expanded_password);

        ivArrowAddress = view.findViewById(R.id.iv_arrow_address);
        ivArrowRename = view.findViewById(R.id.iv_arrow_rename);
        ivArrowPassword = view.findViewById(R.id.iv_arrow_password);

        tvAddressCount = view.findViewById(R.id.tv_address_count);
        btnToggleAddAddress = view.findViewById(R.id.btn_toggle_add_address);
        layoutAddressList = view.findViewById(R.id.layout_address_list_container);

        etEditName = view.findViewById(R.id.et_edit_profile_name);
        etEditPhone = view.findViewById(R.id.et_edit_profile_phone);
        MaterialButton btnSaveName = view.findViewById(R.id.btn_save_profile_info);

        etCurrentPass = view.findViewById(R.id.et_current_password);
        etNewPass = view.findViewById(R.id.et_new_password);
        etConfirmPass = view.findViewById(R.id.et_confirm_new_password);
        btnSavePass = view.findViewById(R.id.btn_save_password);

        MaterialButton btnLogout = view.findViewById(R.id.btn_profile_logout);

        // Quan sát dữ liệu người dùng từ AuthViewModel
        observeViewModel();

        btnEditNameTrigger.setOnClickListener(v -> toggleSection(layoutExpandedRename, ivArrowRename));

        menuItemOrders.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToTab(R.id.nav_orders);
            }
        });

        menuItemAddress.setOnClickListener(v -> toggleSection(layoutExpandedAddress, ivArrowAddress));
        menuItemRename.setOnClickListener(v -> toggleSection(layoutExpandedRename, ivArrowRename));
        menuItemPassword.setOnClickListener(v -> {
            if (!authViewModel.isPasswordSignInUser()) return;
            toggleSection(layoutExpandedPassword, ivArrowPassword);
        });

        // Gọi SAU khi gán setOnClickListener (vì setOnClickListener tự bật lại clickable=true)
        updatePasswordSectionState();
        authViewModel.refreshPasswordEligibility();

        btnToggleAddAddress.setOnClickListener(v -> showAddressDialog(null));

        // Đổi thông tin tên, số điện thoại lưu Firestore
        btnSaveName.setOnClickListener(v -> {
            if (!NetworkMonitor.isConnected(requireContext())) {
                Toast.makeText(requireContext(), NetworkMonitor.NO_INTERNET_MESSAGE, Toast.LENGTH_SHORT).show();
                return;
            }
            String name = etEditName.getText() != null ? etEditName.getText().toString().trim() : "";
            String phone = etEditPhone.getText() != null ? etEditPhone.getText().toString().trim() : "";

            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng nhập họ tên hợp lệ", Toast.LENGTH_SHORT).show();
                return;
            }

            authViewModel.updateDisplayName(name, phone);
        });

        // Đổi mật khẩu qua FirebaseUser
        btnSavePass.setOnClickListener(v -> {
            if (!NetworkMonitor.isConnected(requireContext())) {
                Toast.makeText(requireContext(), NetworkMonitor.NO_INTERNET_MESSAGE, Toast.LENGTH_SHORT).show();
                return;
            }
            String newPass = etNewPass.getText() != null ? etNewPass.getText().toString().trim() : "";
            String confirmPass = etConfirmPass.getText() != null ? etConfirmPass.getText().toString().trim() : "";

            if (newPass.length() < 6) {
                Toast.makeText(requireContext(), "Mật khẩu mới phải có ít nhất 6 ký tự!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPass.equals(confirmPass)) {
                Toast.makeText(requireContext(), "Mật khẩu xác nhận chưa khớp!", Toast.LENGTH_SHORT).show();
                return;
            }

            authViewModel.changePassword(newPass);
            etCurrentPass.setText("");
            etNewPass.setText("");
            etConfirmPass.setText("");
        });

        // Đăng xuất và chuyển về màn hình đăng nhập
        btnLogout.setOnClickListener(v -> {
            authViewModel.logout();
            Toast.makeText(requireContext(), "Đã đăng xuất khỏi tài khoản", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(requireContext(), AuthActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    private void observeViewModel() {
        authViewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            if (swipeRefreshProfile != null) {
                swipeRefreshProfile.setRefreshing(false);
            }
            if (user != null) {
                tvName.setText(user.getName());
                tvEmail.setText(user.getEmail());
                if (tvPhone != null) {
                    String p = user.getPhone();
                    if (p != null && !p.trim().isEmpty()) {
                        tvPhone.setText("SĐT chính: " + p.trim());
                    } else {
                        tvPhone.setText("Chưa cập nhật SĐT");
                    }
                }
                etEditName.setText(user.getName());
                etEditPhone.setText(user.getPhone());
            }
            updatePasswordSectionState();
        });

        authViewModel.getCanChangePassword().observe(getViewLifecycleOwner(), eligible -> updatePasswordSectionState());

        authViewModel.getAddresses().observe(getViewLifecycleOwner(), this::renderAddressList);

        authViewModel.getSuccessMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
                if (layoutExpandedRename != null) layoutExpandedRename.setVisibility(View.GONE);
                if (ivArrowRename != null) ivArrowRename.setRotation(0);
                if (layoutExpandedPassword != null) layoutExpandedPassword.setVisibility(View.GONE);
                if (ivArrowPassword != null) ivArrowPassword.setRotation(0);
                authViewModel.clearStatus();
            }
        });

        authViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
                authViewModel.clearStatus();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (authViewModel != null) {
            authViewModel.refreshPasswordEligibility();
        }
        updatePasswordSectionState();
    }

    private void updatePasswordSectionState() {
        boolean canChangePassword = authViewModel != null && authViewModel.isPasswordSignInUser();
        if (menuItemPassword != null) {
            menuItemPassword.setEnabled(canChangePassword);
            menuItemPassword.setClickable(canChangePassword);
            menuItemPassword.setFocusable(canChangePassword);
            menuItemPassword.setAlpha(canChangePassword ? 1.0f : 0.45f);
        }
        if (tvPasswordSubtitle != null) {
            tvPasswordSubtitle.setVisibility(canChangePassword ? View.GONE : View.VISIBLE);
        }
        if (ivArrowPassword != null) {
            ivArrowPassword.setVisibility(canChangePassword ? View.VISIBLE : View.GONE);
            if (!canChangePassword) {
                ivArrowPassword.setRotation(0);
            }
        }
        if (!canChangePassword && layoutExpandedPassword != null) {
            layoutExpandedPassword.setVisibility(View.GONE);
        }
        if (etCurrentPass != null) etCurrentPass.setEnabled(canChangePassword);
        if (etNewPass != null) etNewPass.setEnabled(canChangePassword);
        if (etConfirmPass != null) etConfirmPass.setEnabled(canChangePassword);
        if (btnSavePass != null) btnSavePass.setEnabled(canChangePassword);
    }

    private void toggleSection(LinearLayout layout, ImageView arrow) {
        if (layout.getVisibility() == View.VISIBLE) {
            layout.setVisibility(View.GONE);
            arrow.animate().rotation(0).setDuration(200).start();
        } else {
            // Close others
            layoutExpandedAddress.setVisibility(View.GONE);
            layoutExpandedRename.setVisibility(View.GONE);
            layoutExpandedPassword.setVisibility(View.GONE);
            ivArrowAddress.setRotation(0);
            ivArrowRename.setRotation(0);
            ivArrowPassword.setRotation(0);

            layout.setVisibility(View.VISIBLE);
            arrow.animate().rotation(90).setDuration(200).start();
        }
    }

    private void renderAddressList(List<Address> addresses) {
        layoutAddressList.removeAllViews();
        int count = addresses != null ? addresses.size() : 0;
        tvAddressCount.setText("Địa chỉ đã lưu (" + count + ")");

        if (addresses == null) return;

        for (Address addr : addresses) {
            LinearLayout itemCard = new LinearLayout(requireContext());
            itemCard.setOrientation(LinearLayout.HORIZONTAL);
            itemCard.setGravity(Gravity.CENTER_VERTICAL);
            itemCard.setPadding(32, 24, 32, 24);
            itemCard.setBackgroundResource(R.drawable.bg_banner_gradient);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, 16);
            itemCard.setLayoutParams(lp);

            if (addr.isDefault()) {
                itemCard.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.md_theme_light_secondaryContainer));
            } else {
                itemCard.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.md_theme_light_surface));
            }

            LinearLayout textCol = new LinearLayout(requireContext());
            textCol.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams lpText = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            textCol.setLayoutParams(lpText);

            LinearLayout titleRow = new LinearLayout(requireContext());
            titleRow.setOrientation(LinearLayout.HORIZONTAL);

            TextView tvLabel = new TextView(requireContext());
            tvLabel.setText(addr.getLabel());
            tvLabel.setTextSize(13);
            tvLabel.setTypeface(null, android.graphics.Typeface.BOLD);
            tvLabel.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_light_onSurface));
            titleRow.addView(tvLabel);

            if (addr.isDefault()) {
                TextView tvBadge = new TextView(requireContext());
                tvBadge.setText("Mặc định");
                tvBadge.setTextSize(10);
                tvBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_light_onPrimary));
                tvBadge.setBackgroundResource(R.drawable.bg_banner_gradient);
                tvBadge.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.md_theme_light_primary));
                tvBadge.setPadding(12, 4, 12, 4);
                LinearLayout.LayoutParams lpBadge = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lpBadge.setMargins(16, 0, 0, 0);
                tvBadge.setLayoutParams(lpBadge);
                titleRow.addView(tvBadge);
            }
            textCol.addView(titleRow);

            TextView tvDetail = new TextView(requireContext());
            tvDetail.setText(addr.getDetail());
            tvDetail.setTextSize(11);
            tvDetail.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_light_onSurfaceVariant));
            tvDetail.setPadding(0, 4, 0, 0);
            textCol.addView(tvDetail);

            if (addr.getPhone() != null && !addr.getPhone().trim().isEmpty()) {
                TextView tvItemPhone = new TextView(requireContext());
                tvItemPhone.setText("SĐT: " + addr.getPhone().trim());
                tvItemPhone.setTextSize(11);
                tvItemPhone.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_light_onSurfaceVariant));
                tvItemPhone.setPadding(0, 2, 0, 0);
                textCol.addView(tvItemPhone);
            }

            itemCard.addView(textCol);

            LinearLayout actionRow = new LinearLayout(requireContext());
            actionRow.setOrientation(LinearLayout.HORIZONTAL);
            actionRow.setGravity(Gravity.CENTER_VERTICAL);

            ImageButton btnEdit = new ImageButton(requireContext());
            btnEdit.setImageResource(R.drawable.ic_edit);
            btnEdit.setImageTintList(ContextCompat.getColorStateList(requireContext(), R.color.md_theme_light_primary));
            btnEdit.setBackgroundResource(android.R.color.transparent);
            btnEdit.setPadding(12, 12, 12, 12);
            btnEdit.setContentDescription("Sửa địa chỉ");
            btnEdit.setOnClickListener(v -> showAddressDialog(addr));
            actionRow.addView(btnEdit);

            if (!addr.isDefault()) {
                ImageButton btnDelete = new ImageButton(requireContext());
                btnDelete.setImageResource(R.drawable.ic_delete);
                btnDelete.setImageTintList(ContextCompat.getColorStateList(requireContext(), R.color.md_theme_light_error));
                btnDelete.setBackgroundResource(android.R.color.transparent);
                btnDelete.setPadding(12, 12, 12, 12);
                btnDelete.setContentDescription("Xóa địa chỉ");
                btnDelete.setOnClickListener(v -> {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Xóa địa chỉ")
                            .setMessage("Bạn có chắc muốn xóa địa chỉ \"" + addr.getLabel() + "\" không?")
                            .setPositiveButton("Xóa", (dialog, which) -> authViewModel.deleteAddress(addr.getId()))
                            .setNegativeButton("Hủy", null)
                            .show();
                });
                actionRow.addView(btnDelete);
            }

            RadioButton rbDefault = new RadioButton(requireContext());
            rbDefault.setChecked(addr.isDefault());
            rbDefault.setClickable(false);
            actionRow.addView(rbDefault);

            itemCard.addView(actionRow);

            itemCard.setOnClickListener(v -> {
                if (!addr.isDefault()) {
                    authViewModel.setDefaultAddress(addr.getId());
                }
            });

            layoutAddressList.addView(itemCard);
        }
    }

    private void showAddressDialog(@Nullable Address addrToEdit) {
        boolean isEdit = addrToEdit != null;
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_address, null);
        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_address_title);
        TextView tvSubtitle = dialogView.findViewById(R.id.tv_dialog_address_subtitle);
        EditText etLabel = dialogView.findViewById(R.id.et_dialog_address_label);
        EditText etPhone = dialogView.findViewById(R.id.et_dialog_address_phone);
        EditText etDetail = dialogView.findViewById(R.id.et_dialog_address_detail);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_dialog_cancel);
        MaterialButton btnSave = dialogView.findViewById(R.id.btn_dialog_save);

        if (isEdit) {
            tvTitle.setText("Chỉnh sửa địa chỉ");
            tvSubtitle.setText("Cập nhật thông tin giao hàng của bạn");
            btnSave.setText("Lưu thay đổi");
            etLabel.setText(addrToEdit.getLabel());
            etPhone.setText(addrToEdit.getPhone());
            etDetail.setText(addrToEdit.getDetail());
        } else {
            tvTitle.setText("Thêm địa chỉ mới");
            tvSubtitle.setText("Nhập thông tin giao hàng mới của bạn");
            btnSave.setText("Thêm địa chỉ");
            User u = authViewModel.getCurrentUser().getValue();
            String mainPhone = (u != null && u.getPhone() != null) ? u.getPhone().trim() : "";
            etPhone.setText(mainPhone);
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
                Toast.makeText(requireContext(), "Vui lòng nhập số điện thoại nhận hàng hợp lệ (10-11 chữ số)", Toast.LENGTH_SHORT).show();
                return;
            }
            if (detail.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng nhập địa chỉ chi tiết", Toast.LENGTH_SHORT).show();
                return;
            }

            if (label.isEmpty()) label = isEdit ? "Địa chỉ" : "Địa chỉ mới";

            if (isEdit) {
                Address updated = new Address(addrToEdit.getId(), label, detail, phone, addrToEdit.isDefault());
                authViewModel.updateAddress(updated);
            } else {
                authViewModel.addAddress(label, detail, phone);
            }
            dialog.dismiss();
        });

        dialog.show();
    }
}
