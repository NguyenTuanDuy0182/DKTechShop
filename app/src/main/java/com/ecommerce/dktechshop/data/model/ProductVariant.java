package com.ecommerce.dktechshop.data.model;

import java.io.Serializable;

public class ProductVariant implements Serializable {
    private String name = "";
    private String colorHex = "";
    private long extraPrice = 0L;
    private int stock = 0;
    private boolean explicitStock = false;

    public ProductVariant() {}

    public ProductVariant(String name, String colorHex, long extraPrice) {
        this.name = name != null ? name : "";
        this.colorHex = colorHex != null ? colorHex : "";
        this.extraPrice = extraPrice;
        this.stock = 0;
        this.explicitStock = false;
    }

    public ProductVariant(String name, String colorHex, long extraPrice, int stock) {
        this.name = name != null ? name : "";
        this.colorHex = colorHex != null ? colorHex : "";
        this.extraPrice = extraPrice;
        this.stock = Math.max(0, stock);
        this.explicitStock = true;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null ? name : "";
    }

    public String getColorHex() {
        return colorHex;
    }

    public void setColorHex(String colorHex) {
        this.colorHex = colorHex != null ? colorHex : "";
    }

    public long getExtraPrice() {
        return extraPrice;
    }

    public void setExtraPrice(long extraPrice) {
        this.extraPrice = extraPrice;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = Math.max(0, stock);
        this.explicitStock = true;
    }

    public boolean hasExplicitStock() {
        return explicitStock;
    }
}
