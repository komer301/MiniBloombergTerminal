package com.minibloomberg.logic;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.minibloomberg.data.Stock;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONObject;

import com.minibloomberg.ui.WatchlistPanel;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * Handles real-time price updates using Finnhub's WebSocket API.
 * Responsible for:
 * - Subscribing/unsubscribing to live ticker feeds
 * - Storing latest price and change percentage
 * - Updating the UI (watchlist) with current trade data
 */
public class LivePriceManager {

    // Stores latest trade data for each subscribed ticker
    private final Map<String, TradeData> tickerData = new ConcurrentHashMap<>();

    // Stores previous closing prices for change percentage calculation
    private final Map<String, Double> basePrices = new ConcurrentHashMap<>();

    // Reference to the UI panel that displays watchlist tickers
    private final WatchlistPanel watchlistPanel;

    // WebSocket client for real-time communication
    private WebSocketClient client;

    // Schedules periodic tasks (UI updates and WebSocket pings)
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public LivePriceManager(WatchlistPanel panel) {
        this.watchlistPanel = panel;
    }

    /**
     * Establishes WebSocket connection, subscribes to tickers,
     * and sets up periodic updates to the watchlist panel.
     */
    public void connect() {
        Dotenv dotenv = Dotenv.load();
        String apiKey = dotenv.get("FINNHUB_API_KEY");

        try {
            client = new WebSocketClient(new URI("wss://ws.finnhub.io?token=" + apiKey)) {

                @Override
                public void onOpen(ServerHandshake handshake) {
                    // Resubscribe to all tickers on (re)connect
                    for (String symbol : tickerData.keySet()) {
                        send("{\"type\":\"subscribe\",\"symbol\":\"" + symbol + "\"}");
                    }
                }

                @Override
                public void onMessage(String message) {
                    JSONObject json = new JSONObject(message);
                    if (!json.has("data")) return;

                    JSONArray dataArray = json.getJSONArray("data");
                    for (int i = 0; i < dataArray.length(); i++) {
                        JSONObject trade = dataArray.getJSONObject(i);
                        String symbol = trade.getString("s");
                        double price = trade.getDouble("p");

                        // Calculate percent change from previous close
                        double base = basePrices.get(symbol);
                        double change = ((price - base) / base) * 100.0;

                        // Update trade data
                        tickerData.put(symbol, new TradeData(price, change));
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.err.printf("[WebSocket] Closed: Code=%d Reason=%s Remote=%b%n", code, reason, remote);
                }

                @Override
                public void onError(Exception ex) {
                    System.err.println("[WebSocket] Error occurred:");
                }
            };

            client.connect();

        } catch (Exception e) {
            System.err.println("[Critical] Failed to establish WebSocket connection:");
        }

        // Periodically push latest trade updates to the UI
        executor.scheduleAtFixedRate(() -> {
            for (Map.Entry<String, TradeData> entry : tickerData.entrySet()) {
                String symbol = entry.getKey();
                TradeData trade = entry.getValue();
                watchlistPanel.updateTicker(symbol, trade.price(), trade.changePercent());
            }
        }, 0, 1, TimeUnit.SECONDS);

        // Periodically ping WebSocket to keep connection alive
        executor.scheduleAtFixedRate(() -> {
            if (client != null && client.isOpen()) {
                client.send("ping");
            }
        }, 0, 30, TimeUnit.SECONDS);
    }

    /**
     * Adds a new ticker to the live feed and updates the watchlist.
     */
    public void addTicker(Stock stock) {
        String symbol = stock.symbol();

        if (!tickerData.containsKey(symbol)) {
            double price = stock.currentPrice();
            double percentChange = stock.percentChange();

            tickerData.put(symbol, new TradeData(price, percentChange));
            basePrices.put(symbol, stock.previousClose());

            watchlistPanel.updateTicker(symbol, price, percentChange);

            if (client != null && client.isOpen()) {
                client.send("{\"type\":\"subscribe\",\"symbol\":\"" + symbol + "\"}");
            }
        }
    }

    /**
     * Removes a ticker from the live feed and UI.
     */
    public void removeTicker(String symbol) {
        tickerData.remove(symbol);
        basePrices.remove(symbol);

        if (client != null && client.isOpen()) {
            client.send("{\"type\":\"unsubscribe\",\"symbol\":\"" + symbol + "\"}");
        }

        watchlistPanel.removeTicker(symbol);
    }

    /**
     * Checks if a ticker is currently subscribed.
     */
    public boolean containsTicker(String symbol) {
        return tickerData.containsKey(symbol);
    }

    /**
     * Immutable trade data structure.
     *
     * @param price         Current price of the ticker
     * @param changePercent Percent change from previous close
     */
    public record TradeData(double price, double changePercent) {
    }
}
