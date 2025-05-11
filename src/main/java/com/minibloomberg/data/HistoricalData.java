package com.minibloomberg.data;

import java.util.List;

/**
 * Encapsulates historical time series data for a stock.
 *
 * @param timestamps   A list of epoch timestamps (in seconds) for each historical point.
 * @param closePrices  A list of closing prices corresponding to each timestamp.
 */
public record HistoricalData(List<Long> timestamps, List<Double> closePrices) {
}
