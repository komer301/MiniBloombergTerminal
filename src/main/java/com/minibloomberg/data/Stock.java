package com.minibloomberg.data;

public record Stock(String symbol, String companyName, double currentPrice, double change, double percentChange,
                    double previousClose, double dayHigh, double dayLow) {
}
