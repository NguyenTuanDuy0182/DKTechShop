package com.ecommerce.dktechshop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.ecommerce.dktechshop.data.model.CartItem;
import com.ecommerce.dktechshop.data.model.Order;
import com.ecommerce.dktechshop.data.model.OrderItem;
import com.ecommerce.dktechshop.data.model.Product;
import com.ecommerce.dktechshop.data.model.ProductVariant;
import com.ecommerce.dktechshop.data.repository.OrderRepository;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OrderTest {

    @Test
    public void testOrderItemGettersAndSetters() {
        OrderItem item = new OrderItem("prod_1", "Logitech GPX2", "Trắng", 3590000L, 2);
        assertEquals("prod_1", item.getProductId());
        assertEquals("Logitech GPX2", item.getProductName());
        assertEquals("Trắng", item.getVariant());
        assertEquals(3590000L, item.getPrice());
        assertEquals(2, item.getQuantity());

        item.setQuantity(3);
        assertEquals(3, item.getQuantity());
    }

    @Test
    public void testOrderCreationAndProperties() {
        List<OrderItem> items = new ArrayList<>();
        items.add(new OrderItem("prod_1", "Logitech GPX2", "Trắng", 3590000L, 1));

        Order order = new Order(
                "TG-2026-9999",
                "user_test_123",
                "Hôm nay • 12:00",
                "Đang xử lý",
                "COD",
                "123 Nguyễn Huệ, Q1, TP.HCM",
                3590000L,
                items
        );

        assertEquals("TG-2026-9999", order.getOrderId());
        assertEquals("user_test_123", order.getUserId());
        assertEquals("Đang xử lý", order.getStatus());
        assertEquals("COD", order.getPaymentMethod());
        assertEquals("123 Nguyễn Huệ, Q1, TP.HCM", order.getShippingAddress());
        assertEquals(3590000L, order.getTotalAmount());
        assertEquals(1, order.getItems().size());
    }

    @Test
    public void testAggregateOrderItemQuantitiesForStockRestore() {
        List<OrderItem> items = Arrays.asList(
                new OrderItem("prod_mouse", "Mouse", "Đen", 1000000L, 1),
                new OrderItem("prod_mouse", "Mouse", "Trắng", 1000000L, 2),
                new OrderItem("prod_kb", "Keyboard", "Đỏ", 2000000L, 1)
        );

        Map<String, Long> restoreMap = OrderRepository.aggregateOrderItemQuantities(items);
        assertEquals(2, restoreMap.size());
        assertEquals(Long.valueOf(3L), restoreMap.get("prod_mouse"));
        assertEquals(Long.valueOf(1L), restoreMap.get("prod_kb"));
    }

    @Test
    public void testProductParsesPerVariantStockFromFirestoreMap() {
        Map<String, Object> v1 = new HashMap<>();
        v1.put("name", "Đen");
        v1.put("colorHex", "#1E1E1E");
        v1.put("extraPrice", 0L);
        v1.put("stock", 0L);

        Map<String, Object> v2 = new HashMap<>();
        v2.put("name", "Trắng");
        v2.put("colorHex", "#FAFAFA");
        v2.put("extraPrice", 50000L);
        v2.put("stock", 4L);

        Map<String, Object> v3 = new HashMap<>();
        v3.put("name", "Hồng Magenta");
        v3.put("colorHex", "#E91E63");
        v3.put("extraPrice", 150000L);
        v3.put("stock", 3L);

        Product product = new Product();
        product.setId("prod_logi_gpx2");
        product.setName("Logitech G Pro X Superlight 2");
        product.setVariants(Arrays.asList(v1, v2, v3));

        assertTrue(product.hasExplicitVariantStock());
        assertEquals(0, product.resolveVariantStock("Đen"));
        assertEquals(4, product.resolveVariantStock("Trắng"));
        assertEquals(3, product.resolveVariantStock("Hồng Magenta"));
        assertEquals(7, product.computeTotalStock());
        assertEquals("Trắng", product.findFirstAvailableVariant().getName());
    }

    @Test
    public void testVariantStockDeductionAndCancelRestorePerColor() {
        Product product = new Product(
                "prod_logi_g102",
                "Bàn phím cơ DareU EK87",
                "DareU",
                "keyboard",
                "Bàn phím cơ",
                450000L, 550000L, 18, 4.8, 12, 36,
                new HashMap<>(),
                Arrays.asList(
                        new ProductVariant("Đen", "#1E1E1E", 0L, 12),
                        new ProductVariant("Trắng", "#F5F5F5", 0L, 10),
                        new ProductVariant("Xanh Dương", "#005AC1", 20000L, 8),
                        new ProductVariant("Tím Lilac", "#9C27B0", 30000L, 6)
                )
        );

        // 1. Đặt hàng gồm 2 màu khác nhau của cùng 1 sản phẩm: 2 Đen + 3 Tím Lilac
        List<CartItem> cartItems = Arrays.asList(
                new CartItem("c1", "prod_logi_g102", "Bàn phím cơ DareU EK87", "keyboard", 450000L, "Đen", 2),
                new CartItem("c2", "prod_logi_g102", "Bàn phím cơ DareU EK87", "keyboard", 480000L, "Tím Lilac", 3)
        );
        Map<String, Map<String, Integer>> groupedCart = OrderRepository.aggregateCartItemVariantQuantities(cartItems);
        Map<String, Integer> deductions = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> e : groupedCart.get("prod_logi_g102").entrySet()) {
            deductions.put(e.getKey(), -e.getValue());
        }

        OrderRepository.StockUpdateResult afterOrder = OrderRepository.applyVariantStockDeltas(product, deductions, true);
        assertTrue(afterOrder.hasVariants);
        assertEquals(31, afterOrder.totalStock);
        assertEquals(10, afterOrder.serializedVariants.get(0).get("stock")); // Đen: 12 - 2
        assertEquals(10, afterOrder.serializedVariants.get(1).get("stock")); // Trắng: giữ nguyên 10
        assertEquals(8, afterOrder.serializedVariants.get(2).get("stock"));  // Xanh Dương: giữ nguyên 8
        assertEquals(3, afterOrder.serializedVariants.get(3).get("stock"));  // Tím Lilac: 6 - 3

        // Cập nhật trạng thái sản phẩm như sau khi ghi lên Firestore
        product.setVariants(afterOrder.serializedVariants);
        product.setStock(afterOrder.totalStock);

        // 2. Thử đặt 4 Tím Lilac (chỉ còn 3) dù tổng kho vẫn còn 31 -> phải từ chối
        Map<String, Integer> overOrder = new LinkedHashMap<>();
        overOrder.put("Tím Lilac", -4);
        try {
            OrderRepository.applyVariantStockDeltas(product, overOrder, true);
            fail("Phải báo lỗi khi số lượng đặt vượt tồn kho của màu Tím Lilac");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("Tím Lilac"));
            assertTrue(expected.getMessage().contains("3"));
        }

        // 3. Hủy đơn hàng cũ -> hoàn đúng 2 Đen + 3 Tím Lilac về đúng từng màu
        List<OrderItem> cancelledItems = Arrays.asList(
                new OrderItem("prod_logi_g102", "Bàn phím cơ DareU EK87", "Đen", 450000L, 2),
                new OrderItem("prod_logi_g102", "Bàn phím cơ DareU EK87", "Tím Lilac", 480000L, 3)
        );
        Map<String, Map<String, Integer>> groupedCancel = OrderRepository.aggregateOrderItemVariantQuantities(cancelledItems);
        OrderRepository.StockUpdateResult afterCancel = OrderRepository.applyVariantStockDeltas(
                product, groupedCancel.get("prod_logi_g102"), false);

        assertEquals(36, afterCancel.totalStock);
        assertEquals(12, afterCancel.serializedVariants.get(0).get("stock")); // Đen: 10 + 2
        assertEquals(10, afterCancel.serializedVariants.get(1).get("stock")); // Trắng: 10
        assertEquals(8, afterCancel.serializedVariants.get(2).get("stock"));  // Xanh Dương: 8
        assertEquals(6, afterCancel.serializedVariants.get(3).get("stock"));  // Tím Lilac: 3 + 3
    }
}
