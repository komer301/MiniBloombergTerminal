package com.minibloomberg.data;

import java.util.List;

public record HistoricalData(List<Long> timestamps, List<Double> closePrices) {
}
