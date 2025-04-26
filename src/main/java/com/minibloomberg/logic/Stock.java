package com.minibloomberg.logic;

public class Stock {
    public final String symbol;
    public final String companyName;
    public final double currentPrice;
    public final double change;
    public final double percentChange;
    public final double previousClose;
    public final double dayHigh;
    public final double dayLow;

    public Stock(String symbol, String companyName, double currentPrice, double change,
                         double percentChange, double previousClose, double dayHigh, double dayLow) {
        this.symbol = symbol;
        this.companyName = companyName;
        this.currentPrice = currentPrice;
        this.change = change;
        this.percentChange = percentChange;
        this.previousClose = previousClose;
        this.dayHigh = dayHigh;
        this.dayLow = dayLow;
    }
}
