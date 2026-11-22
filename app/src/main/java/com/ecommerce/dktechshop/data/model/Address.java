package com.ecommerce.dktechshop.data.model;

import java.io.Serializable;

public class Address implements Serializable {
    private String id = "";
    private String label = "";
    private String detail = "";
    private boolean isDefault = false;

    // Mandatory no-arg constructor for Firestore deserialization
    public Address() {}

    public Address(String id, String label, String detail, boolean isDefault) {
        this.id = id;
        this.label = label;
        this.detail = detail;
        this.isDefault = isDefault;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean aDefault) { isDefault = aDefault; }
}
