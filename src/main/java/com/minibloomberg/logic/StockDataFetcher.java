package com.minibloomberg.logic;

import io.github.cdimascio.dotenv.Dotenv;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class StockDataFetcher {
    private static final Dotenv dotenv = Dotenv.load();
    private static final String apiKey = dotenv.get("FINNHUB_API_KEY");
    private static final String quoteUrl = "https://finnhub.io/api/v1/quote";
    private static final String profileUrl = "https://finnhub.io/api/v1/stock/profile2";

    private static double getSafeDouble(JSONObject json, String key) {
        if (json.has(key) && !json.isNull(key)) {
            return json.getDouble(key);
        }
        return Double.NaN; // or any default invalid value you want
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

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            return new JSONObject(response.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
