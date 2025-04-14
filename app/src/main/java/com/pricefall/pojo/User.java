package com.pricefall.pojo;

public class User {
    public String id;
    public String name;
    public String phone;
    public String photo;
    public String email;
    public String token;
    public boolean userType;

    public String address;
    public PaymentCardInfo paymentCardInfo;


    public static final boolean SELLER = true;
    public static final boolean BUYER = false;

    public User() {

    }

}
