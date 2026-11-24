package com.ecommerce.dktechshop.utils;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Tiện ích định dạng tiền tệ Việt Nam Đồng (VND).
 */
public final class CurrencyUtils {

    private CurrencyUtils() {}

    public static String formatVND(long amount) {
        return NumberFormat.getNumberInstance(Locale.GERMANY).format(amount) + "₫";
    }
}
