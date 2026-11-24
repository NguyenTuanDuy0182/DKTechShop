package com.ecommerce.dktechshop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.ecommerce.dktechshop.data.model.Product;
import com.ecommerce.dktechshop.data.model.ProductVariant;
import com.ecommerce.dktechshop.ui.viewmodel.ShopViewModel;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SearchTest {

    @Test
    public void testStripAccents() {
        assertEquals("chuot", ShopViewModel.stripAccents("chuột"));
        assertEquals("chuot", ShopViewModel.stripAccents("CHUỘT"));
        assertEquals("the nho", ShopViewModel.stripAccents("thẻ nhớ"));
        assertEquals("ban phim co", ShopViewModel.stripAccents("Bàn phím cơ"));
        assertEquals("do", ShopViewModel.stripAccents("Đỏ"));
        assertEquals("den", ShopViewModel.stripAccents("đen"));
    }

    @Test
    public void testSearchMouseByAccentedAndUnaccentedKeywords() {
        Map<String, String> specs = new HashMap<>();
        specs.put("Cảm biến", "HERO 2 (32.000 DPI)");
        specs.put("Trọng lượng", "60g siêu nhẹ");

        Product mouse = new Product(
                "prod_logi_gpx2",
                "Logitech G Pro X Superlight 2",
                "Logitech G",
                "mouse",
                "Chuột Gaming",
                3290000L, 3890000L, 15, 4.9, 328, 45,
                specs,
                Arrays.asList(
                        new ProductVariant("Đen", "#000000", 0L),
                        new ProductVariant("Trắng", "#FFFFFF", 0L)
                )
        );
        mouse.setDescription("Chuột gaming không dây siêu nhẹ Logitech G Pro X Superlight 2.");

        // Matches by Vietnamese category label (accented & unaccented)
        assertTrue(ShopViewModel.matchesProduct(mouse, "chuột"));
        assertTrue(ShopViewModel.matchesProduct(mouse, "chuot"));
        assertTrue(ShopViewModel.matchesProduct(mouse, "CHUỘT"));

        // Matches by English category
        assertTrue(ShopViewModel.matchesProduct(mouse, "mouse"));

        // Matches by brand + category combined
        assertTrue(ShopViewModel.matchesProduct(mouse, "chuột logitech"));
        assertTrue(ShopViewModel.matchesProduct(mouse, "logitech chuot"));

        // Matches by spec or variant
        assertTrue(ShopViewModel.matchesProduct(mouse, "hero 2"));
        assertTrue(ShopViewModel.matchesProduct(mouse, "chuột trắng"));
        assertTrue(ShopViewModel.matchesProduct(mouse, "chuot trang"));

        // Negative check
        assertFalse(ShopViewModel.matchesProduct(mouse, "bàn phím"));
        assertFalse(ShopViewModel.matchesProduct(mouse, "tai nghe"));
    }

    @Test
    public void testSearchMemoryCardWhenNameDoesNotContainCategory() {
        // Product where name is in English without the word "thẻ nhớ" in the title
        Product sdCard = new Product(
                "prod_sandisk_128",
                "SanDisk Extreme Pro 128GB MicroSDXC",
                "SanDisk",
                "storage",
                "Thẻ nhớ",
                450000L, 550000L, 18, 4.8, 110, 80,
                new HashMap<>(),
                Collections.emptyList()
        );
        sdCard.setDescription("Thẻ nhớ MicroSD tốc độ cao 200MB/s chuẩn U3 V30 A2 cho flycam và điện thoại.");

        // Should match "thẻ nhớ" and "the nho"
        assertTrue(ShopViewModel.matchesProduct(sdCard, "thẻ nhớ"));
        assertTrue(ShopViewModel.matchesProduct(sdCard, "the nho"));
        assertTrue(ShopViewModel.matchesProduct(sdCard, "the nho sandisk"));
        assertTrue(ShopViewModel.matchesProduct(sdCard, "sandisk 128gb"));

        // Negative check
        assertFalse(ShopViewModel.matchesProduct(sdCard, "chuột"));
    }
}
