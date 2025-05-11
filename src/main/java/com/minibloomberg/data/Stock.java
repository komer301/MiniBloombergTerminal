package com.minibloomberg.data;

/**
 * Represents a snapshot of a stock's current market data.
 *
 * @param symbol         The stock ticker symbol (e.g. AAPL, TSLA).
 * @param companyName    The full company name.
 * @param currentPrice   The current trading price.
 * @param change         The absolute price change from the previous close.
 * @param percentChange  The percentage price change from the previous close.
 * @param previousClose  The closing price from the previous trading day.
 * @param dayHigh        The highest price during the current trading day.
 * @param dayLow         The lowest price during the current trading day.
 */
public record Stock(String symbol, String companyName, double currentPrice, double change, double percentChange,
                    double previousClose, double dayHigh, double dayLow) {
}
