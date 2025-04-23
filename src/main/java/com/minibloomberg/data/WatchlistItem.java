package com.minibloomberg.data;

public class WatchlistItem {
    private final String symbol;
    private Double initialPrice = null;
    private double latestPrice;

    public WatchlistItem(String symbol) {
        this.symbol = symbol;
    }

    public void updatePrice(double price) {
        if (initialPrice == null) {
            initialPrice = price;
        }
        this.latestPrice = price;
    }

    public double getChangePercent() {
        if (initialPrice == null) return 0;
        return ((latestPrice - initialPrice) / initialPrice) * 100;
    }

    public String getSymbol() {
        return symbol;
    }

    public double getLatestPrice() {
        return latestPrice;
    }
}
