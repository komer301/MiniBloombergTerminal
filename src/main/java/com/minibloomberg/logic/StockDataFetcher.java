package com.minibloomberg.logic;

import com.minibloomberg.data.HistoricalData;
import io.github.cdimascio.dotenv.Dotenv;
import org.jetbrains.annotations.NotNull;
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

public class StockDataFetcher {
    private static final Dotenv dotenv = Dotenv.load();
    private static final String apiKey = dotenv.get("FINNHUB_API_KEY");
    private static final String alphaVantageApiKey = dotenv.get("ALPHA_VANTAGE_API_KEY");
    private static final String quoteUrl = "https://finnhub.io/api/v1/quote";
    private static final String profileUrl = "https://finnhub.io/api/v1/stock/profile2";

    @NotNull
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

    private static double getSafeDouble(JSONObject json, String key) {
        if (json.has(key) && !json.isNull(key)) {
            return json.getDouble(key);
        }
        return Double.NaN;
    }

    public static Stock fetchStockSnapshot(String ticker) {
        try {
            JSONObject quoteData = fetchJson(quoteUrl + "?symbol=" + ticker + "&token=" + apiKey);
            JSONObject profileData = fetchJson(profileUrl + "?symbol=" + ticker + "&token=" + apiKey);

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

            if (Double.isNaN(currentPrice ) || currentPrice == 0.0) {
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

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

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
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

//    AlphaVantage
    public static HistoricalData fetchHistoricalData(String symbol) {
        try {
            String urlString = "https://www.alphavantage.co/query"
                    + "?function=TIME_SERIES_DAILY"
                    + "&symbol=" + symbol
                    + "&outputsize=full"
                    + "&apikey=" + alphaVantageApiKey;

            JSONObject response = fetchJson(urlString);
            if (response == null) {
                System.err.println("Failed to fetch data for " + symbol);
                return null;
            }

            System.out.println(response.toString(2));
            JSONObject timeSeries = response.optJSONObject("Time Series (Daily)");
            if (timeSeries == null) {
                System.err.println("Invalid Alpha Vantage response for " + symbol);
                return null;
            }

            List<Long> timestamps = new ArrayList<>();
            List<Double> closePrices = new ArrayList<>();

            List<String> dates = new ArrayList<>(timeSeries.keySet());
            dates.sort(String::compareTo);

            for (String date : dates) {
                JSONObject dayData = timeSeries.getJSONObject(date);

                double close = dayData.getDouble("4. close");

                long epochTime = LocalDate.parse(date)
                        .atStartOfDay(ZoneOffset.UTC)
                        .toEpochSecond();

                timestamps.add(epochTime);
                closePrices.add(close);
            }

            return new HistoricalData(timestamps, closePrices);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

//    Test graph
//    public static HistoricalData fetchHistoricalData(String symbol) {
//        try {
//            InputStream inputStream = StockDataFetcher.class.getClassLoader().getResourceAsStream("mock_data.json");
//            if (inputStream == null) {
//                throw new FileNotFoundException("mock_data.json not found in resources!");
//            }
//            String response = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
//
//            JSONObject responseJson = new JSONObject(response);
//
//            JSONObject timeSeries = responseJson.optJSONObject("Time Series (Daily)");
//            if (timeSeries == null) {
//                System.err.println("Invalid mock data response for " + symbol);
//                return null;
//            }
//
//            List<Long> timestamps = new ArrayList<>();
//            List<Double> closePrices = new ArrayList<>();
//
//            List<String> dates = new ArrayList<>(timeSeries.keySet());
//            dates.sort(String::compareTo);
//
//            for (String date : dates) {
//                JSONObject dayData = timeSeries.getJSONObject(date);
//                double close = dayData.getDouble("4. close");
//
//                long epochTime = java.time.LocalDate.parse(date)
//                        .atStartOfDay(java.time.ZoneId.systemDefault())
//                        .toEpochSecond();
//
//                timestamps.add(epochTime);
//                closePrices.add(close);
//            }
//
//            return new HistoricalData(timestamps, closePrices);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            return null;
//        }
//    }
}