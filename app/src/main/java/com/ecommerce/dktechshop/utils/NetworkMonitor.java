package com.ecommerce.dktechshop.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;

import com.ecommerce.dktechshop.R;

import java.util.HashSet;
import java.util.Set;

/**
 * Theo dõi trạng thái kết nối mạng theo thời gian thực và hỗ trợ hiển thị banner mất mạng.
 */
public class NetworkMonitor extends LiveData<Boolean> {

    public static final String NO_INTERNET_MESSAGE = "Không có kết nối mạng. Vui lòng kiểm tra Wi-Fi hoặc dữ liệu di động.";

    private static NetworkMonitor instance;
    private final Context appContext;
    private final ConnectivityManager connectivityManager;
    private final Set<Network> availableNetworks = new HashSet<>();

    private final ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(@NonNull Network network) {
            synchronized (availableNetworks) {
                availableNetworks.add(network);
            }
            postValue(true);
        }

        @Override
        public void onLost(@NonNull Network network) {
            boolean hasAny;
            synchronized (availableNetworks) {
                availableNetworks.remove(network);
                hasAny = !availableNetworks.isEmpty();
            }
            postValue(hasAny);
        }

        @Override
        public void onUnavailable() {
            synchronized (availableNetworks) {
                availableNetworks.clear();
            }
            postValue(false);
        }

        @Override
        public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities caps) {
            boolean hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            boolean hasAny;
            synchronized (availableNetworks) {
                if (hasInternet) {
                    availableNetworks.add(network);
                } else {
                    availableNetworks.remove(network);
                }
                hasAny = !availableNetworks.isEmpty();
            }
            postValue(hasAny);
        }
    };

    private NetworkMonitor(Context context) {
        this.appContext = context.getApplicationContext();
        this.connectivityManager = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public static synchronized NetworkMonitor getInstance(Context context) {
        if (instance == null) {
            instance = new NetworkMonitor(context);
        }
        return instance;
    }

    public static boolean isConnected(@Nullable Context context) {
        if (context == null) return false;
        ConnectivityManager cm = (ConnectivityManager) context.getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        Network activeNetwork = cm.getActiveNetwork();
        if (activeNetwork == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(activeNetwork);
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    @Override
    protected void onActive() {
        super.onActive();
        synchronized (availableNetworks) {
            availableNetworks.clear();
            if (connectivityManager != null) {
                Network active = connectivityManager.getActiveNetwork();
                if (active != null && isConnected(appContext)) {
                    availableNetworks.add(active);
                }
            }
        }
        setValue(isConnected(appContext));

        if (connectivityManager != null) {
            try {
                connectivityManager.registerDefaultNetworkCallback(networkCallback);
            } catch (Exception ignored) {}
        }
    }

    @Override
    protected void onInactive() {
        super.onInactive();
        if (connectivityManager != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception ignored) {}
        }
    }

    /**
     * Gắn banner thông báo "Không có kết nối mạng" tự động ẩn/hiện theo trạng thái mạng.
     */
    public static void bindOfflineBanner(@NonNull LifecycleOwner owner,
                                         @NonNull Context context,
                                         @Nullable View bannerView,
                                         @Nullable Runnable onRetry) {
        if (bannerView == null) return;

        final boolean[] wasOffline = {false};

        View btnRetry = bannerView.findViewById(R.id.btn_no_internet_retry);
        if (btnRetry != null) {
            btnRetry.setOnClickListener(v -> {
                if (isConnected(context)) {
                    bannerView.setVisibility(View.GONE);
                    if (onRetry != null) onRetry.run();
                } else {
                    Toast.makeText(context, NO_INTERNET_MESSAGE, Toast.LENGTH_SHORT).show();
                }
            });
        }

        getInstance(context).observe(owner, connected -> {
            boolean online = Boolean.TRUE.equals(connected);
            if (!online) {
                wasOffline[0] = true;
                if (bannerView.getVisibility() != View.VISIBLE) {
                    bannerView.setAlpha(0f);
                    bannerView.setVisibility(View.VISIBLE);
                    bannerView.animate().alpha(1f).setDuration(200).start();
                }
            } else {
                bannerView.setVisibility(View.GONE);
                if (wasOffline[0]) {
                    wasOffline[0] = false;
                    if (onRetry != null) onRetry.run();
                }
            }
        });
    }
}
