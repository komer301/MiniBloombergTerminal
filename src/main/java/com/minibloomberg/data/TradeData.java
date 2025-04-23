package com.minibloomberg.data;

public class TradeData {
    private final double price;
    private final double changePercent;

    public TradeData(double price, double changePercent) {
        this.price = price;
        this.changePercent = changePercent;
    }

    public double getPrice() {
        return price;
    }

    public double getChangePercent() {
        return changePercent;
    }
}
