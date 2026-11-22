package com.ecommerce.dktechshop;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.ecommerce.dktechshop.data.repository.MockRepository;
import com.ecommerce.dktechshop.ui.cart.CartFragment;
import com.ecommerce.dktechshop.ui.home.HomeFragment;
import com.ecommerce.dktechshop.ui.orders.OrdersFragment;
import com.ecommerce.dktechshop.ui.profile.ProfileFragment;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity implements MockRepository.OnDataChangeListener {

    private BottomNavigationView bottomNav;
    private BadgeDrawable cartBadge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        // Set up Cart badge
        cartBadge = bottomNav.getOrCreateBadge(R.id.nav_cart);
        updateCartBadge();

        // Default or directed tab
        if (savedInstanceState == null) {
            int targetTab = getIntent().getIntExtra("navigate_tab", R.id.nav_home);
            navigateToTab(targetTab);
        }

        MockRepository.getInstance().addListener(this);
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
                // If item is already selected, setSelectedItemId won't trigger onItemSelectedListener,
                // so we manually load the fragment to ensure it is displayed on launch
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
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .commit();
    }

    private void updateCartBadge() {
        if (cartBadge != null) {
            int count = MockRepository.getInstance().getCartCount();
            if (count > 0) {
                cartBadge.setVisible(true);
                cartBadge.setNumber(count);
            } else {
                cartBadge.setVisible(false);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MockRepository.getInstance().removeListener(this);
    }

    @Override
    public void onDataChanged() {
        runOnUiThread(this::updateCartBadge);
    }
}
