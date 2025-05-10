package com.minibloomberg.data;

import java.util.List;

public class HistoricalData {
    public final List<Long> timestamps;
    public final List<Double> closePrices;

    public HistoricalData(List<Long> timestamps, List<Double> closePrices) {
        this.timestamps = timestamps;
        this.closePrices = closePrices;
    }
}
