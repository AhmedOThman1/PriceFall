package com.pricefall.pojo;

import android.util.Log;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class Auction {
    public String id;
    public String sellerId;
    public ArrayList<String> images;
    public String title;
    public String category;
    public double startingPrice;
    public double minimumPrice;
    public long startTime;
    public long durationInMillis;
    public int quantity;
    public String description;

    public User seller;
    public int quantitySold;

    public Auction() {
    }

    public Auction(String id, String sellerId, ArrayList<String> images, String title, double startingPrice, double minimumPrice, long startTime, long durationInMillis, int quantity, String description) {
        this.id = id;
        this.sellerId = sellerId;
        this.images = images;
        this.title = title;
        this.startingPrice = startingPrice;
        this.minimumPrice = minimumPrice;
        this.startTime = startTime;
        this.durationInMillis = durationInMillis;
        this.quantity = quantity;
        this.description = description;
    }

    public double getCurrentAuctionPrice(long currentTimeMillis) {
        if (currentTimeMillis < startTime) {
            return startingPrice;
        }

        long endTimeMillis = startTime + durationInMillis;

        if (currentTimeMillis >= endTimeMillis) {
            return minimumPrice;
        }
        //250 kd , 50 kd

        long durationSeconds = durationInMillis / 1000;
        double priceDropPerSecond = (startingPrice - minimumPrice) / durationSeconds;

        long secondsPassed = (currentTimeMillis - startTime) / 1000;
        double currentPrice = startingPrice - (secondsPassed * priceDropPerSecond);

        return Math.max(minimumPrice, currentPrice);
    }

    public String getCurrentAuctionPriceString(long currentTimeMillis) {
        return new DecimalFormat("#.###").format(getCurrentAuctionPrice(currentTimeMillis));
    }

    public long getTimeByPrice(double price) {
        if (price >= startingPrice) {
            return startTime;
        }

        long durationSeconds = durationInMillis / 1000;
        double priceDropPerSecond = (startingPrice - minimumPrice) / durationSeconds;

        if (price <= minimumPrice) {
            return startTime + durationInMillis;
        }

        // seconds it takes to reach the given price
        double secondsToReach = (startingPrice - price) / priceDropPerSecond;

        // convert to millis and add to start time
        long timeAtPrice = startTime + (long) (secondsToReach * 1000);

        return timeAtPrice;
    }

    public static final int UPCOMING = 1;
    public static final int LIVE = 2;
    public static final int DONE = 3;

    public int getAuctionStatus(long currentTimeMillis) {
        if (currentTimeMillis < startTime)
            return UPCOMING;

        long endTimeMillis = startTime + durationInMillis;

        if (currentTimeMillis >= endTimeMillis)
            return DONE;

        Log.w("Quantity", "" + quantity + "," + quantitySold);

        if (quantity == quantitySold)
            return DONE;

        return LIVE;
    }

    public int getIdAsInt() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Auction{" +
                "id='" + id + '\'' +
                ", quantitySold=" + quantitySold +
                ", quantity=" + quantity +
                '}';
    }
}
