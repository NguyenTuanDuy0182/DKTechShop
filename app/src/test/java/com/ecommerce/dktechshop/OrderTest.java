package com.ecommerce.dktechshop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.ecommerce.dktechshop.data.model.Order;
import com.ecommerce.dktechshop.data.model.OrderItem;
import com.ecommerce.dktechshop.data.repository.OrderRepository;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
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
}
