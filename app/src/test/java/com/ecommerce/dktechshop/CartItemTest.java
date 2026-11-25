package com.ecommerce.dktechshop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.ecommerce.dktechshop.data.model.CartItem;
import com.ecommerce.dktechshop.data.repository.CartRepository;
import com.ecommerce.dktechshop.ui.cart.CartViewModel;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CartItemTest {

    @Test
    public void testCartItemGettersAndSetters() {
        CartItem item = new CartItem(
                "cart_prod_1",
                "prod_1",
                "Chuột Gaming",
                "mouse",
                1500000L,
                "Đen",
                2,
                "https://example.com/mouse.png"
        );

        assertEquals("cart_prod_1", item.getItemId());
        assertEquals("prod_1", item.getProductId());
        assertEquals("Chuột Gaming", item.getProductName());
        assertEquals("mouse", item.getCategory());
        assertEquals(1500000L, item.getPrice());
        assertEquals("Đen", item.getVariant());
        assertEquals(2, item.getQuantity());
        assertEquals("https://example.com/mouse.png", item.getThumbnailUrl());

        item.setQuantity(3);
        assertEquals(3, item.getQuantity());

        item.setThumbnailUrl("https://example.com/mouse2.png");
        assertEquals("https://example.com/mouse2.png", item.getThumbnailUrl());
    }

    @Test
    public void testGenerateItemIdNormalization() {
        String id1 = CartRepository.generateItemId("prod_logi_gpx2", "Black - Wireless 2.4GHz");
        assertNotNull(id1);
        assertEquals("prod_logi_gpx2_Black_-_Wireless_2_4GHz", id1);

        String id2 = CartRepository.generateItemId("prod_sony_xm5", null);
        assertEquals("prod_sony_xm5_default", id2);
    }

    @Test
    public void testSelectedCartItemsFilterAndTotalCalculation() {
        CartItem mouse = new CartItem("item_1", "p1", "Chuột Gaming", "mouse", 1000000L, "Đen", 2);
        CartItem kb = new CartItem("item_2", "p2", "Bàn phím cơ", "keyboard", 1500000L, "Trắng", 1);
        CartItem headset = new CartItem("item_3", "p3", "Tai nghe", "headphone", 2000000L, "Đen", 1);
        List<CartItem> allItems = Arrays.asList(mouse, kb, headset);

        // Select 2 out of 3 items
        Set<String> selected = new HashSet<>(Arrays.asList("item_1", "item_3"));
        List<CartItem> filtered = CartViewModel.filterSelectedItems(allItems, selected);
        assertEquals(2, filtered.size());
        assertEquals(4000000L, CartViewModel.calculateSelectedTotal(allItems, selected));

        // Select 0 items
        assertEquals(0, CartViewModel.filterSelectedItems(allItems, Collections.emptySet()).size());
        assertEquals(0L, CartViewModel.calculateSelectedTotal(allItems, Collections.emptySet()));

        // Select all 3 items
        Set<String> allSelected = new HashSet<>(Arrays.asList("item_1", "item_2", "item_3"));
        assertEquals(3, CartViewModel.filterSelectedItems(allItems, allSelected).size());
        assertEquals(5500000L, CartViewModel.calculateSelectedTotal(allItems, allSelected));
    }
}
