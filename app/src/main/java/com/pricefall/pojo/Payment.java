package com.pricefall.pojo;

import java.util.ArrayList;

public class Payment {
    public String id;
    public String userId;
    public String auctionId;
    public int quantity;
    public double price;
    public double totalAmount;
    public Long date;
    public ArrayList<Status> status;

    public Auction auction;
    public User user;

    public Payment() {
        status = new ArrayList<>();
    }

    public static class Status {
        public String status;
        public long date;

        public Status(String status, long date) {
            this.status = status;
            this.date = date;
        }

        public Status() {
        }
    }
}

