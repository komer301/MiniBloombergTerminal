package com.minibloomberg.logic;

import com.minibloomberg.data.HistoricalData;
import com.minibloomberg.data.Stock;
import io.github.cdimascio.dotenv.Dotenv;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Responsible for fetching stock data including live snapshot and historical prices.
 * Uses Finnhub for quotes and Alpha Vantage for historical and fuzzy data.
 */
public class StockDataFetcher {

    // Load API keys from .env file
    private static final Dotenv dotenv = Dotenv.load();
    private static final String finnhubApiKey = dotenv.get("FINNHUB_API_KEY");
    private static final String alphaVantageApiKey = dotenv.get("ALPHA_API_KEY");
    private static final String finnhubUrl = "https://finnhub.io/api/v1/";

    /**
     * Converts the raw HTTP response body into a JSONObject.
     */
    private static JSONObject getJsonObject(HttpURLConnection conn) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            response.append(line);
        }
        in.close();
        return new JSONObject(response.toString());
    }

    /**
     * Safely retrieves a double value from the given JSON object using the specified key.
     * Returns NaN if key is missing or null.
     */
    private static double getSafeDouble(JSONObject json, String key) {
        if (json.has(key) && !json.isNull(key)) {
            return json.getDouble(key);
        }
        return Double.NaN;
    }

    /**
     * Fetches a snapshot of the stock's current state (price, change, highs/lows).
     * Uses Finnhub for real-time data.
     */
    public static Stock fetchStockSnapshot(String ticker) {
        try {
            JSONObject quoteData = fetchJson(finnhubUrl + "quote?symbol=" + ticker + "&token=" + finnhubApiKey);
            JSONObject profileData = fetchJson(finnhubUrl + "stock/profile2?symbol=" + ticker + "&token=" + finnhubApiKey);

            if (quoteData == null || profileData == null) {
                System.err.println("Failed to fetch data for ticker: " + ticker);
                return null;
            }

            String companyName = profileData.optString("name", "N/A");
            double currentPrice = getSafeDouble(quoteData, "c");
            double change = getSafeDouble(quoteData, "d");
            double percentChange = getSafeDouble(quoteData, "dp");
            double previousClose = getSafeDouble(quoteData, "pc");
            double dayHigh = getSafeDouble(quoteData, "h");
            double dayLow = getSafeDouble(quoteData, "l");

            // Skip invalid entries
            if (Double.isNaN(currentPrice) || currentPrice == 0.0) {
                return null;
            }

            return new Stock(
                    ticker,
                    companyName,
                    currentPrice,
                    change,
                    percentChange,
                    previousClose,
                    dayHigh,
                    dayLow
            );

        } catch (JSONException e) {
            System.err.println("Failed to fetch or parse stock data for ticker: " + ticker);
            return new Stock(ticker, "Unavailable", 0, 0, 0, 0, 0, 0);
        }
    }

    /**
     * Sends an HTTP GET request to the specified URL and parses the JSON response.
     * Returns null if the request fails or encounters an exception.
     */
    private static JSONObject fetchJson(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                System.err.println("Request failed: " + urlString);
                return null;
            }

            return getJsonObject(conn);
        } catch (IOException e) {
            System.err.println("IOException occurred while fetching URL: " + urlString);
            System.err.println("Error message: " + e.getMessage());
            return null;
        }
    }

    /**
     * Fetches full historical daily closing price data for a stock.
     * Returns a HistoricalData object containing timestamps and prices.
     */
    public static HistoricalData fetchHistoricalData(String symbol) {
        try {
            String urlString = "https://www.alphavantage.co/query"
                    + "?function=TIME_SERIES_DAILY_ADJUSTED"
                    + "&symbol=" + symbol
                    + "&outputsize=full"
                    + "&apikey=" + alphaVantageApiKey;

            JSONObject response = fetchJson(urlString);
            if (response == null) {
                System.err.println("Failed to fetch data for " + symbol);
                return null;
            }

            JSONObject timeSeries = response.optJSONObject("Time Series (Daily)");
            if (timeSeries == null) {
                System.err.println("Invalid Alpha Vantage response for " + symbol);
                return null;
            }

            List<Long> timestamps = new ArrayList<>();
            List<Double> closePrices = new ArrayList<>();

            // Alpha Vantage returns dates as string keys; sort to ensure order
            List<String> dates = new ArrayList<>(timeSeries.keySet());
            dates.sort(String::compareTo);

            // Parse each date and store the adjusted close
            for (String date : dates) {
                JSONObject dayData = timeSeries.getJSONObject(date);
                double close = dayData.getDouble("5. adjusted close");

                long epochTime = LocalDate.parse(date)
                        .atStartOfDay(ZoneOffset.UTC)
                        .toEpochSecond();

                timestamps.add(epochTime);
                closePrices.add(close);
            }

            return new HistoricalData(timestamps, closePrices);

        } catch (Exception e) {
            System.err.printf("Failed to fetch or parse historical data for %s: %s%n", symbol, e.getMessage());
            return null;
        }
    }
}
