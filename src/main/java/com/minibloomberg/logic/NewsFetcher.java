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

public class NewsFetcher {
    public static List<NewsArticle> fetchLatestNews() {
        List<NewsArticle> articles = new ArrayList<>();
        try {
            Dotenv dotenv = Dotenv.load();
            String apiKey = dotenv.get("FINNHUB_API_KEY");

            JSONArray arr = getObjects(apiKey);
            for (int i = 0; i < 50; i++) {
                JSONObject item = arr.getJSONObject(i);

                String timePublished = item.has("datetime") ? String.valueOf(item.getLong("datetime")) : "";
                articles.add(new NewsArticle(
                    truncateSummary(item.getString("headline"),70),
                    item.optString("url", ""),
                    item.optString("source", ""),
                    timePublished,
                    truncateSummary(item.optString("summary", ""), 150)
                ));
            }

         } catch (JSONException e) {
                System.err.println("[NewsFetcher] JSON parsing error: " + e.getMessage());
        }
            
        catch (Exception e) {
            System.err.println("[NewsFetcher] Failed to fetch news:");
        }
        return articles;
    }

    private static JSONArray getObjects(String apiKey) throws IOException {
        String baseUrl = "https://finnhub.io/api/v1/news";
        String params = "category=general&token=" + apiKey;
        String fullUrl = baseUrl + "?" + params;

        URL url = new URL(fullUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder json = new StringBuilder();
        String inputLine;
        while ((inputLine = in.readLine()) != null) json.append(inputLine);
        in.close();

        return new JSONArray(json.toString());
    }

    public static String truncateSummary(String summary, int maxLen) {
        if (summary == null) return "";
        if (summary.length() <= maxLen) return summary;
        return summary.substring(0, maxLen) + "...";
    }
    
}
