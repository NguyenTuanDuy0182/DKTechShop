package com.ecommerce.dktechshop.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.ecommerce.dktechshop.MainActivity;
import com.ecommerce.dktechshop.R;
import com.ecommerce.dktechshop.data.model.Address;
import com.ecommerce.dktechshop.data.model.User;
import com.ecommerce.dktechshop.data.repository.MockRepository;
import com.ecommerce.dktechshop.ui.auth.AuthActivity;
import com.google.android.material.button.MaterialButton;

public class ProfileFragment extends Fragment implements MockRepository.OnDataChangeListener {

    private TextView tvName;
    private TextView tvEmail;

    // Accordions
    private LinearLayout layoutExpandedAddress;
    private LinearLayout layoutExpandedRename;
    private LinearLayout layoutExpandedPassword;
    private ImageView ivArrowAddress;
    private ImageView ivArrowRename;
    private ImageView ivArrowPassword;

    // Address section views
    private TextView tvAddressCount;
    private LinearLayout layoutFormAddAddress;
    private LinearLayout layoutAddressList;
    private EditText etNewAddressLabel;
    private EditText etNewAddressDetail;

    // Rename section views
    private EditText etEditName;
    private EditText etEditPhone;

    // Password section views
    private EditText etCurrentPass;
    private EditText etNewPass;
    private EditText etConfirmPass;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvName = view.findViewById(R.id.tv_profile_name);
        tvEmail = view.findViewById(R.id.tv_profile_email);
        TextView btnEditNameTrigger = view.findViewById(R.id.btn_profile_edit_name_trigger);

        LinearLayout menuItemOrders = view.findViewById(R.id.menu_item_orders);
        LinearLayout menuItemAddress = view.findViewById(R.id.menu_item_address_header);
        LinearLayout menuItemRename = view.findViewById(R.id.menu_item_rename_header);
        LinearLayout menuItemPassword = view.findViewById(R.id.menu_item_password_header);

        layoutExpandedAddress = view.findViewById(R.id.layout_expanded_address);
        layoutExpandedRename = view.findViewById(R.id.layout_expanded_rename);
        layoutExpandedPassword = view.findViewById(R.id.layout_expanded_password);

        ivArrowAddress = view.findViewById(R.id.iv_arrow_address);
        ivArrowRename = view.findViewById(R.id.iv_arrow_rename);
        ivArrowPassword = view.findViewById(R.id.iv_arrow_password);

        tvAddressCount = view.findViewById(R.id.tv_address_count);
        MaterialButton btnToggleAddAddress = view.findViewById(R.id.btn_toggle_add_address);
        layoutFormAddAddress = view.findViewById(R.id.layout_form_add_address);
        layoutAddressList = view.findViewById(R.id.layout_address_list_container);
        etNewAddressLabel = view.findViewById(R.id.et_new_address_label);
        etNewAddressDetail = view.findViewById(R.id.et_new_address_detail);
        MaterialButton btnSaveAddress = view.findViewById(R.id.btn_save_new_address);

        etEditName = view.findViewById(R.id.et_edit_profile_name);
        etEditPhone = view.findViewById(R.id.et_edit_profile_phone);
        MaterialButton btnSaveName = view.findViewById(R.id.btn_save_profile_info);

        etCurrentPass = view.findViewById(R.id.et_current_password);
        etNewPass = view.findViewById(R.id.et_new_password);
        etConfirmPass = view.findViewById(R.id.et_confirm_new_password);
        MaterialButton btnSavePass = view.findViewById(R.id.btn_save_password);

        MaterialButton btnLogout = view.findViewById(R.id.btn_profile_logout);

        // Populate user info
        refreshUserInfo();

        btnEditNameTrigger.setOnClickListener(v -> toggleSection(layoutExpandedRename, ivArrowRename));

        menuItemOrders.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToTab(R.id.nav_orders);
            }
        });

        menuItemAddress.setOnClickListener(v -> toggleSection(layoutExpandedAddress, ivArrowAddress));
        menuItemRename.setOnClickListener(v -> toggleSection(layoutExpandedRename, ivArrowRename));
        menuItemPassword.setOnClickListener(v -> toggleSection(layoutExpandedPassword, ivArrowPassword));

        btnToggleAddAddress.setOnClickListener(v -> {
            if (layoutFormAddAddress.getVisibility() == View.VISIBLE) {
                layoutFormAddAddress.setVisibility(View.GONE);
                btnToggleAddAddress.setText("+ Thêm địa chỉ");
            } else {
                layoutFormAddAddress.setVisibility(View.VISIBLE);
                btnToggleAddAddress.setText("Đóng form");
            }
        });

        btnSaveAddress.setOnClickListener(v -> {
            String label = etNewAddressLabel.getText() != null ? etNewAddressLabel.getText().toString().trim() : "";
            String detail = etNewAddressDetail.getText() != null ? etNewAddressDetail.getText().toString().trim() : "";

            if (detail.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng nhập địa chỉ chi tiết", Toast.LENGTH_SHORT).show();
                return;
            }

            if (label.isEmpty()) label = "Địa chỉ mới";
            MockRepository.getInstance().addAddress(label, detail);
            etNewAddressDetail.setText("");
            layoutFormAddAddress.setVisibility(View.GONE);
            btnToggleAddAddress.setText("+ Thêm địa chỉ");
            Toast.makeText(requireContext(), "Đã thêm và đặt làm địa chỉ mặc định!", Toast.LENGTH_SHORT).show();
            renderAddressList();
        });

        btnSaveName.setOnClickListener(v -> {
            String name = etEditName.getText() != null ? etEditName.getText().toString().trim() : "";
            String phone = etEditPhone.getText() != null ? etEditPhone.getText().toString().trim() : "";

            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng nhập họ tên hợp lệ", Toast.LENGTH_SHORT).show();
                return;
            }

            MockRepository.getInstance().updateProfile(name, phone);
            layoutExpandedRename.setVisibility(View.GONE);
            ivArrowRename.setRotation(0);
            Toast.makeText(requireContext(), "Đã đổi tên người dùng thành công!", Toast.LENGTH_SHORT).show();
            refreshUserInfo();
        });

        btnSavePass.setOnClickListener(v -> {
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

            etCurrentPass.setText("");
            etNewPass.setText("");
            etConfirmPass.setText("");
            layoutExpandedPassword.setVisibility(View.GONE);
            ivArrowPassword.setRotation(0);
            Toast.makeText(requireContext(), "Đã đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show();
        });

        btnLogout.setOnClickListener(v -> {
            MockRepository.getInstance().getCurrentUser().setLoggedIn(false);
            Toast.makeText(requireContext(), "Đã đăng xuất khỏi tài khoản", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(requireContext(), AuthActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        renderAddressList();
        MockRepository.getInstance().addListener(this);
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

    private void refreshUserInfo() {
        User user = MockRepository.getInstance().getCurrentUser();
        tvName.setText(user.getName());
        tvEmail.setText(user.getEmail());
        etEditName.setText(user.getName());
        etEditPhone.setText(user.getPhone());
    }

    private void renderAddressList() {
        layoutAddressList.removeAllViews();
        User user = MockRepository.getInstance().getCurrentUser();
        tvAddressCount.setText("Địa chỉ đã lưu (" + user.getAddresses().size() + ")");

        for (Address addr : user.getAddresses()) {
            LinearLayout itemCard = new LinearLayout(requireContext());
            itemCard.setOrientation(LinearLayout.HORIZONTAL);
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
            tvDetail.setPadding(0, 6, 0, 0);
            textCol.addView(tvDetail);

            itemCard.addView(textCol);

            RadioButton rbDefault = new RadioButton(requireContext());
            rbDefault.setChecked(addr.isDefault());
            rbDefault.setClickable(false);
            itemCard.addView(rbDefault);

            itemCard.setOnClickListener(v -> {
                MockRepository.getInstance().setDefaultAddress(addr.getId());
                renderAddressList();
                Toast.makeText(requireContext(), "Đã đổi địa chỉ giao hàng mặc định", Toast.LENGTH_SHORT).show();
            });

            layoutAddressList.addView(itemCard);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        MockRepository.getInstance().removeListener(this);
    }

    @Override
    public void onDataChanged() {
        if (isAdded()) {
            refreshUserInfo();
            renderAddressList();
        }
    }
}
