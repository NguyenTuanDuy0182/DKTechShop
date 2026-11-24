package com.ecommerce.dktechshop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.ecommerce.dktechshop.data.model.CartItem;
import com.ecommerce.dktechshop.data.repository.CartRepository;

import org.junit.Test;

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
}
