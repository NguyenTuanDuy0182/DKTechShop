package com.ecommerce.dktechshop.data.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class User implements Serializable {
    private String uid = "";
    private String name = "";
    private String email = "";
    private String phone = "";
    private String address = "";
    private List<Address> addresses = new ArrayList<>();
    private boolean isLoggedIn = false;

    // Mandatory no-arg constructor for Firestore deserialization
    public User() {}

    public User(String uid, String name, String email, String phone, String address, List<Address> addresses, boolean isLoggedIn) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.addresses = addresses != null ? addresses : new ArrayList<>();
        this.isLoggedIn = isLoggedIn;
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public List<Address> getAddresses() { return addresses; }
    public void setAddresses(List<Address> addresses) { this.addresses = addresses; }
    public boolean isLoggedIn() { return isLoggedIn; }
    public void setLoggedIn(boolean loggedIn) { isLoggedIn = loggedIn; }
}
