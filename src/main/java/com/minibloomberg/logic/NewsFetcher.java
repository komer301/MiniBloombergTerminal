package com.minibloomberg.logic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.minibloomberg.data.NewsArticle;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * Fetches the latest financial news articles from the Finnhub API.
 * Converts API responses into a list of NewsArticle records.
 */
public class NewsFetcher {

    /**
     * Retrieves and parses up to 50 news articles from the Finnhub general news category.
     *
     * @return a list of parsed NewsArticle objects
     */
    public static List<NewsArticle> fetchLatestNews() {
        List<NewsArticle> articles = new ArrayList<>();
        try {
            // Load API key from .env
            Dotenv dotenv = Dotenv.load();
            String apiKey = dotenv.get("FINNHUB_API_KEY");

            // Fetch raw news JSON array
            JSONArray arr = getObjects(apiKey);

            // Parse up to 50 news articles
            for (int i = 0; i < 50; i++) {
                JSONObject item = arr.getJSONObject(i);

                // Optional handling for timestamp
                String timePublished = item.has("datetime") ? String.valueOf(item.getLong("datetime")) : "";

                // Create NewsArticle record and add to list
                articles.add(new NewsArticle(
                        truncateSummary(item.getString("headline"), 70),
                        item.optString("url", ""),
                        item.optString("source", ""),
                        timePublished,
                        truncateSummary(item.optString("summary", ""), 150)
                ));
            }

        } catch (JSONException e) {
            System.err.println("[NewsFetcher] JSON parsing error: " + e.getMessage());

        } catch (Exception e) {
            System.err.println("[NewsFetcher] Failed to fetch news:");
        }

        return articles;
    }

    /**
     * Makes an HTTP GET request to the Finnhub news endpoint and returns the result as a JSONArray.
     *
     * @param apiKey your Finnhub API key
     * @return JSONArray of news articles
     * @throws IOException if there's a problem reading the API response
     */
    private static JSONArray getObjects(String apiKey) throws IOException {
        String baseUrl = "https://finnhub.io/api/v1/news";
        String params = "category=general&token=" + apiKey;
        String fullUrl = baseUrl + "?" + params;

        URL url = new URL(fullUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        // Read response from API
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder json = new StringBuilder();
        String inputLine;
        while ((inputLine = in.readLine()) != null) json.append(inputLine);
        in.close();

        return new JSONArray(json.toString());
    }

    /**
     * Truncates a given summary string to a specified length and appends "..." if truncated.
     *
     * @param summary the original summary string
     * @param maxLen  the maximum length allowed
     * @return the truncated string with ellipsis if needed
     */
    public static String truncateSummary(String summary, int maxLen) {
        if (summary == null) return "";
        if (summary.length() <= maxLen) return summary;
        return summary.substring(0, maxLen) + "...";
    }
}
