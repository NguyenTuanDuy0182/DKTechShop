package com.ecommerce.dktechshop.data.repository;

import com.ecommerce.dktechshop.data.model.Address;
import com.ecommerce.dktechshop.data.model.CartItem;
import com.ecommerce.dktechshop.data.model.Order;
import com.ecommerce.dktechshop.data.model.OrderItem;
import com.ecommerce.dktechshop.data.model.Product;
import com.ecommerce.dktechshop.data.model.User;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MockRepository {
    private static MockRepository instance;

    private final List<Product> products = new ArrayList<>();
    private final List<CartItem> cartItems = new ArrayList<>();
    private final List<Order> orders = new ArrayList<>();
    private final List<String> favorites = new ArrayList<>();
    private User currentUser;

    public interface OnDataChangeListener {
        void onDataChanged();
    }

    private final List<OnDataChangeListener> listeners = new ArrayList<>();

    public static synchronized MockRepository getInstance() {
        if (instance == null) {
            instance = new MockRepository();
        }
        return instance;
    }

    private MockRepository() {
        initMockData();
    }

    private void initMockData() {
        // Product 1: Logitech G Pro X Superlight 2
        Map<String, String> specs1 = new LinkedHashMap<>();
        specs1.put("Cảm biến", "HERO 2 (32.000 DPI)");
        specs1.put("Trọng lượng", "60g siêu nhẹ");
        specs1.put("Kết nối", "LIGHTSPEED Wireless 2.4GHz");
        specs1.put("Switch", "LIGHTFORCE Hybrid Optical");
        specs1.put("Thời lượng pin", "95 giờ liên tục");
        products.add(new Product(
                "prod_logi_gpx2",
                "Logitech G Pro X Superlight 2",
                "Logitech G",
                "mouse",
                "Chuột Gaming",
                3590000,
                4190000,
                14,
                4.9,
                328,
                24,
                specs1,
                Arrays.asList("Đen - Wireless 2.4GHz", "Trắng - Wireless 2.4GHz", "Hồng Magenta - Wireless")
        ));

        // Product 2: Sony WH-1000XM5 ANC
        Map<String, String> specs2 = new LinkedHashMap<>();
        specs2.put("Driver âm thanh", "30mm Carbon Fiber");
        specs2.put("Chống ồn", "ANC 8 Micro + Chip V1");
        specs2.put("Kết nối", "Bluetooth 5.2 / LDAC / 3.5mm");
        specs2.put("Trọng lượng", "250g đeo êm ái");
        specs2.put("Thời lượng pin", "30 giờ (Sạc 3p = 3h)");
        products.add(new Product(
                "prod_sony_xm5",
                "Sony WH-1000XM5 ANC",
                "Sony",
                "headphone",
                "Tai nghe",
                6490000,
                7990000,
                19,
                4.9,
                512,
                15,
                specs2,
                Arrays.asList("Đen Nhám - Bluetooth", "Bạc Platinum - Bluetooth")
        ));

        // Product 3: Keychron Q1 Pro
        Map<String, String> specs3 = new LinkedHashMap<>();
        specs3.put("Layout", "75% Nhôm CNC nguyên khối");
        specs3.put("Switch", "Keychron K Pro Banana");
        specs3.put("Kết nối", "Bluetooth 5.1 / Cáp Type-C");
        specs3.put("Keycap", "PBT Double-shot KSA");
        specs3.put("Dung lượng pin", "4000 mAh");
        products.add(new Product(
                "prod_keychron_q1",
                "Keychron Q1 Pro QMK/VIA",
                "Keychron",
                "keyboard",
                "Bàn phím",
                4250000,
                4890000,
                13,
                4.8,
                194,
                12,
                specs3,
                Arrays.asList("Carbon Black - Banana Switch", "Shell White - Red Switch")
        ));

        // Product 4: Razer DA V3 Pro
        Map<String, String> specs4 = new LinkedHashMap<>();
        specs4.put("Cảm biến", "Focus Pro 30K Optical");
        specs4.put("Trọng lượng", "63g Công thái học");
        specs4.put("Kết nối", "HyperSpeed Wireless / USB-C");
        specs4.put("Switch", "Optical Gen-3");
        specs4.put("Thời lượng pin", "90 giờ");
        products.add(new Product(
                "prod_razer_dav3",
                "Razer DA V3 Pro Wireless",
                "Razer",
                "mouse",
                "Chuột Gaming",
                3290000,
                3890000,
                15,
                4.8,
                276,
                19,
                specs4,
                Arrays.asList("Đen - Wireless 2.4GHz", "Trắng - Wireless 2.4GHz", "Đen - Special Edition")
        ));

        // Product 5: HyperX Cloud III Wireless
        Map<String, String> specs5 = new LinkedHashMap<>();
        specs5.put("Driver âm thanh", "53mm Angled Neodymium");
        specs5.put("Công nghệ âm", "DTS Spatial Audio");
        specs5.put("Kết nối", "Wireless 2.4GHz Dongle");
        specs5.put("Trọng lượng", "330g (Đệm Memory Foam)");
        specs5.put("Thời lượng pin", "120 giờ");
        products.add(new Product(
                "prod_hyperx_cloud3",
                "HyperX Cloud III Wireless",
                "HyperX",
                "headphone",
                "Tai nghe",
                3190000,
                3790000,
                16,
                4.7,
                163,
                30,
                specs5,
                Arrays.asList("Đen Đỏ - Wireless 2.4GHz", "Đen Full - Wireless 2.4GHz")
        ));

        // Product 6: Corsair K70 RGB TKL
        Map<String, String> specs6 = new LinkedHashMap<>();
        specs6.put("Layout", "Tenkeyless (87 phím) Nhôm");
        specs6.put("Switch", "Cherry MX Speed Silver");
        specs6.put("Kết nối", "Có dây USB-C (8000Hz)");
        specs6.put("Keycap", "PBT Double-shot 1.5mm");
        specs6.put("Tính năng", "Tournament Switch vật lý");
        products.add(new Product(
                "prod_corsair_k70",
                "Corsair K70 RGB TKL",
                "Corsair",
                "keyboard",
                "Bàn phím",
                2890000,
                3490000,
                17,
                4.7,
                142,
                18,
                specs6,
                Arrays.asList("Đen - Cherry MX Speed", "Đen - Cherry MX Red")
        ));

        // Initial Cart items
        cartItems.add(new CartItem(
                "cart_1",
                "prod_logi_gpx2",
                "Logitech G Pro X Superlight 2",
                "mouse",
                3590000,
                "Đen - Wireless 2.4GHz",
                1
        ));
        cartItems.add(new CartItem(
                "cart_2",
                "prod_hyperx_cloud3",
                "HyperX Cloud III Wireless",
                "headphone",
                3190000,
                "Đen Đỏ - Wireless 2.4GHz",
                1
        ));

        // Initial Orders
        List<OrderItem> order1Items = new ArrayList<>();
        order1Items.add(new OrderItem("Sony WH-1000XM5 ANC", "Đen Nhám - Bluetooth", 6490000, 1));
        orders.add(new Order(
                "TG-2026-8841",
                "22/11/2026 • 09:15",
                "Đang giao",
                "VNPay QR",
                "227 Nguyễn Văn Cừ, P.4, Q.5, TP.HCM",
                6490000,
                order1Items
        ));

        List<OrderItem> order2Items = new ArrayList<>();
        order2Items.add(new OrderItem("Keychron Q1 Pro QMK/VIA", "Carbon Black - Banana Switch", 4250000, 1));
        orders.add(new Order(
                "TG-2026-8790",
                "18/11/2026 • 16:40",
                "Hoàn thành",
                "COD",
                "227 Nguyễn Văn Cừ, P.4, Q.5, TP.HCM",
                4250000,
                order2Items
        ));

        // User
        List<Address> addresses = new ArrayList<>();
        addresses.add(new Address("addr_1", "Nhà riêng", "227 Nguyễn Văn Cừ, P.4, Q.5, TP.HCM", true));
        addresses.add(new Address("addr_2", "Văn phòng", "Tòa nhà Bitexco, Q.1, TP.HCM", false));
        currentUser = new User(
                "uid_duy_2026",
                "Nguyễn Tuấn Duy",
                "duy.nguyen@techgear.vn",
                "0909 123 456",
                "227 Nguyễn Văn Cừ, P.4, Q.5, TP.HCM",
                addresses,
                true
        );

        favorites.add("prod_sony_xm5");
    }

    public List<Product> getProducts() { return products; }

    public Product getProductById(String id) {
        for (Product p : products) {
            if (p.getId().equals(id)) return p;
        }
        return products.get(0);
    }

    public List<Product> filterProducts(String category) {
        if (category == null || category.equals("all")) return products;
        List<Product> result = new ArrayList<>();
        for (Product p : products) {
            if (category.equals("under35")) {
                if (p.getPrice() <= 3500000) result.add(p);
            } else if (p.getCategory().equalsIgnoreCase(category)) {
                result.add(p);
            }
        }
        return result;
    }

    public List<Product> searchAndSort(String query, String sortBy) {
        List<Product> list = new ArrayList<>();
        String q = query != null ? query.trim().toLowerCase() : "";
        for (Product p : products) {
            if (q.isEmpty() || p.getName().toLowerCase().contains(q) ||
                    p.getBrand().toLowerCase().contains(q) ||
                    p.getCategoryLabel().toLowerCase().contains(q)) {
                list.add(p);
            }
        }

        if ("price_asc".equals(sortBy)) {
            Collections.sort(list, Comparator.comparingLong(Product::getPrice));
        } else if ("price_desc".equals(sortBy)) {
            Collections.sort(list, (a, b) -> Long.compare(b.getPrice(), a.getPrice()));
        }
        return list;
    }

    public List<CartItem> getCartItems() { return cartItems; }

    public int getCartCount() {
        int total = 0;
        for (CartItem item : cartItems) {
            total += item.getQuantity();
        }
        return total;
    }

    public long getCartTotal() {
        long total = 0;
        for (CartItem item : cartItems) {
            total += item.getPrice() * item.getQuantity();
        }
        return total;
    }

    public void addToCart(Product product, String variant, int qty) {
        for (CartItem item : cartItems) {
            if (item.getProductId().equals(product.getId()) && item.getVariant().equals(variant)) {
                item.setQuantity(item.getQuantity() + qty);
                notifyListeners();
                return;
            }
        }
        cartItems.add(new CartItem(
                "cart_" + System.currentTimeMillis(),
                product.getId(),
                product.getName(),
                product.getCategory(),
                product.getPrice(),
                variant,
                qty
        ));
        notifyListeners();
    }

    public void updateCartItemQty(String itemId, int delta) {
        for (int i = 0; i < cartItems.size(); i++) {
            CartItem item = cartItems.get(i);
            if (item.getItemId().equals(itemId)) {
                int newQty = item.getQuantity() + delta;
                if (newQty <= 0) {
                    cartItems.remove(i);
                } else {
                    item.setQuantity(newQty);
                }
                notifyListeners();
                return;
            }
        }
    }

    public Order createOrder(String paymentMethod) {
        if (cartItems.isEmpty()) return null;
        List<OrderItem> items = new ArrayList<>();
        for (CartItem ci : cartItems) {
            items.add(new OrderItem(ci.getProductName(), ci.getVariant(), ci.getPrice(), ci.getQuantity()));
        }
        String orderId = "TG-2026-" + (1000 + (int)(Math.random() * 9000));
        Order order = new Order(
                orderId,
                "Hôm nay • Vừa xong",
                "Đang xử lý",
                paymentMethod,
                getDefaultAddressDetail(),
                getCartTotal(),
                items
        );
        orders.add(0, order);
        cartItems.clear();
        notifyListeners();
        return order;
    }

    public List<Order> getOrders(String statusFilter) {
        if (statusFilter == null || statusFilter.equals("all")) return orders;
        List<Order> result = new ArrayList<>();
        for (Order o : orders) {
            if (o.getStatus().equalsIgnoreCase(statusFilter)) {
                result.add(o);
            }
        }
        return result;
    }

    public void advanceOrderStatus(String orderId) {
        for (Order o : orders) {
            if (o.getOrderId().equals(orderId)) {
                if ("Đang xử lý".equals(o.getStatus())) {
                    o.setStatus("Đang giao");
                } else if ("Đang giao".equals(o.getStatus())) {
                    o.setStatus("Hoàn thành");
                }
                notifyListeners();
                return;
            }
        }
    }

    public User getCurrentUser() { return currentUser; }

    public String getDefaultAddressDetail() {
        for (Address a : currentUser.getAddresses()) {
            if (a.isDefault()) return a.getDetail();
        }
        return currentUser.getAddress();
    }

    public void setDefaultAddress(String addressId) {
        for (Address a : currentUser.getAddresses()) {
            boolean isDef = a.getId().equals(addressId);
            a.setDefault(isDef);
            if (isDef) {
                currentUser.setAddress(a.getDetail());
            }
        }
        notifyListeners();
    }

    public void addAddress(String label, String detail) {
        for (Address a : currentUser.getAddresses()) {
            a.setDefault(false);
        }
        Address newAddr = new Address("addr_" + System.currentTimeMillis(), label, detail, true);
        currentUser.getAddresses().add(0, newAddr);
        currentUser.setAddress(detail);
        notifyListeners();
    }

    public void updateProfile(String name, String phone) {
        if (name != null && !name.trim().isEmpty()) {
            currentUser.setName(name.trim());
        }
        if (phone != null && !phone.trim().isEmpty()) {
            currentUser.setPhone(phone.trim());
        }
        notifyListeners();
    }

    public boolean isFavorite(String productId) {
        return favorites.contains(productId);
    }

    public void toggleFavorite(String productId) {
        if (favorites.contains(productId)) {
            favorites.remove(productId);
        } else {
            favorites.add(productId);
        }
        notifyListeners();
    }

    public static String formatVND(long amount) {
        return NumberFormat.getNumberInstance(Locale.GERMANY).format(amount) + "₫";
    }

    public void addListener(OnDataChangeListener listener) {
        if (!listeners.contains(listener)) listeners.add(listener);
    }

    public void removeListener(OnDataChangeListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (OnDataChangeListener l : listeners) {
            l.onDataChanged();
        }
    }
}
