package com.ecommerce.dktechshop;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.ecommerce.dktechshop.ui.auth.AuthActivity;
import com.ecommerce.dktechshop.ui.auth.AuthViewModel;
import com.ecommerce.dktechshop.ui.cart.CartFragment;
import com.ecommerce.dktechshop.ui.cart.CartViewModel;
import com.ecommerce.dktechshop.ui.home.HomeFragment;
import com.ecommerce.dktechshop.ui.orders.OrdersFragment;
import com.ecommerce.dktechshop.ui.profile.ProfileFragment;
import com.ecommerce.dktechshop.utils.NetworkMonitor;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private BadgeDrawable cartBadge;
    private CartViewModel cartViewModel;
    private AuthViewModel authViewModel;
    private boolean hasLoadedInitialFragment = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Kiểm tra phiên đăng nhập Firebase Auth
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Intent intent = new Intent(this, AuthActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        // Khởi tạo AuthViewModel để tải thông tin người dùng từ Firestore
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        authViewModel.loadCurrentUser();

        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        bottomNav = findViewById(R.id.bottom_navigation);
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, insets) -> {
            Insets navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            v.setPadding(0, 0, 0, navBar.bottom);
            return insets;
        });

        bottomNav.setOnItemSelectedListener(item -> {
            loadFragmentForId(item.getItemId());
            return true;
        });

        bottomNav.setOnItemReselectedListener(item -> {
            // Do not reload fragment on re-selection to keep current scroll position
        });

        // Set up Cart badge & CartViewModel Realtime Sync
        cartBadge = bottomNav.getOrCreateBadge(R.id.nav_cart);
        cartViewModel = new ViewModelProvider(this).get(CartViewModel.class);
        cartViewModel.getCartCount().observe(this, count -> {
            if (cartBadge != null) {
                if (count != null && count > 0) {
                    cartBadge.setVisible(true);
                    cartBadge.setNumber(count);
                } else {
                    cartBadge.setVisible(false);
                }
            }
        });

        // Giám sát kết nối mạng toàn cục cho tất cả tab chính
        NetworkMonitor.bindOfflineBanner(this, this, findViewById(R.id.banner_no_internet), () -> {
            if (authViewModel != null) {
                authViewModel.loadCurrentUser();
                authViewModel.loadAddresses();
            }
            if (cartViewModel != null) {
                cartViewModel.startListeningCart();
            }
        });

        // Default or directed tab
        if (savedInstanceState == null) {
            int targetTab = getIntent().getIntExtra("navigate_tab", R.id.nav_home);
            navigateToTab(targetTab);
        }
    }

    @Override
    protected void onNewIntent(@androidx.annotation.NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        int targetTab = intent.getIntExtra("navigate_tab", R.id.nav_home);
        navigateToTab(targetTab);
    }

    public void navigateToTab(int menuItemId) {
        if (bottomNav != null) {
            if (bottomNav.getSelectedItemId() == menuItemId) {
                loadFragmentForId(menuItemId);
            } else {
                bottomNav.setSelectedItemId(menuItemId);
            }
        }
    }

    private void loadFragmentForId(int menuItemId) {
        if (menuItemId == R.id.nav_home) {
            switchFragment(new HomeFragment());
        } else if (menuItemId == R.id.nav_cart) {
            switchFragment(new CartFragment());
        } else if (menuItemId == R.id.nav_orders) {
            switchFragment(new OrdersFragment());
        } else if (menuItemId == R.id.nav_profile) {
            switchFragment(new ProfileFragment());
        }
    }

    private void switchFragment(Fragment fragment) {
        androidx.fragment.app.FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        if (hasLoadedInitialFragment) {
            tx.setCustomAnimations(R.anim.fade_in, R.anim.fade_out);
        }
        hasLoadedInitialFragment = true;
        tx.replace(R.id.nav_host_fragment, fragment).commit();
    }
}
