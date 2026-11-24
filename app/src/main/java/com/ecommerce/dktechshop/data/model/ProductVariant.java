package com.ecommerce.dktechshop.data.model;

import java.io.Serializable;

public class ProductVariant implements Serializable {
    private String name = "";
    private String colorHex = "";
    private long extraPrice = 0L;

    public ProductVariant() {}

    public ProductVariant(String name, String colorHex, long extraPrice) {
        this.name = name != null ? name : "";
        this.colorHex = colorHex != null ? colorHex : "";
        this.extraPrice = extraPrice;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColorHex() {
        return colorHex;
    }

    public void setColorHex(String colorHex) {
        this.colorHex = colorHex;
    }

    public long getExtraPrice() {
        return extraPrice;
    }

    public void setExtraPrice(long extraPrice) {
        this.extraPrice = extraPrice;
    }
}
