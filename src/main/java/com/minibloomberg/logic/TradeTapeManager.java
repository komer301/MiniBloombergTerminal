package com.minibloomberg.logic;

import java.awt.Color;
import java.net.URI;
import java.net.URL;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.*;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONObject;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * Manages the trade tape functionality in both real-time (market hours) and simulated (after-hours) modes.
 * Handles top tickers, WebSocket connection for live updates, and switching modes automatically.
 */
public class TradeTapeManager {
    private WebSocketClient client;
    private final BlockingQueue<TradeItem> tradeQueue = new LinkedBlockingQueue<>();
    private final Map<String, Double> previousCloseCache = new HashMap<>();
    private TradeListener listener;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private boolean lastMarketStatus = isMarketOpen();

    /**
     * Different trade item types for styling and simulation behavior.
     */
    public enum TradeType {
        REALTIME, GAINER, LOSER, ACTIVE, HEADER
    }

    /**
     * Listener interface to notify trade and mode updates to the UI.
     */
    public interface TradeListener {
        void onTrade(TradeItem trade);
        void onMarketModeChanged(boolean isAfterHours);
    }

    public void setTradeListener(TradeListener listener) {
        this.listener = listener;
    }

    /**
     * Determines how to start the trade tape depending on market status.
     * Initializes either WebSocket streaming or fallback simulation.
     */
    public void connect() {
        if (isMarketOpen()) {
            connectLiveWebSocket();
        } else {
            loadTopTickersFromAPI();
        }
        startMarketStatusWatcher();
    }

    /**
     * Rechecks market open status every minute and switches data source accordingly.
     */
    private void startMarketStatusWatcher() {
        scheduler.scheduleAtFixedRate(() -> {
            boolean currentStatus = isMarketOpen();
            if (currentStatus != lastMarketStatus) {
                lastMarketStatus = currentStatus;

                if (currentStatus) {
                    if (client == null || !client.isOpen()) {
                        connectLiveWebSocket();
                    }
                    if (listener != null) listener.onMarketModeChanged(false);
                } else {
                    closeWebSocket();
                    loadTopTickersFromAPI();
                    if (listener != null) listener.onMarketModeChanged(true);
                }
            }
        }, 0, 1, TimeUnit.MINUTES);
    }

    /**
     * Determines if the market is currently open based on NYSE hours.
     */
    public boolean isMarketOpen() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("America/New_York"));
        LocalTime currentTime = now.toLocalTime();
        int dayOfWeek = now.getDayOfWeek().getValue(); // 1 = Monday, 7 = Sunday

        boolean isWeekday = dayOfWeek >= 1 && dayOfWeek <= 5;
        boolean isTradingHours = currentTime.isAfter(LocalTime.of(9, 30)) && currentTime.isBefore(LocalTime.of(16, 0));

        return isWeekday && isTradingHours;
    }

    /**
     * Connects to Finnhub WebSocket for real-time trades and subscribes to tickers.
     */
    private void connectLiveWebSocket() {
        Dotenv dotenv = Dotenv.load();
        String apiKey = dotenv.get("WEBSOCKET_API_KEY");

        try {
            client = new WebSocketClient(new URI("wss://ws.finnhub.io?token=" + apiKey)) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    subscribeToTopTickers();
                }

                @Override
                public void onMessage(String message) {
                    JSONObject json = new JSONObject(message);
                    if (!json.has("data")) return;

                    JSONArray dataArray = json.getJSONArray("data");
                    for (int i = 0; i < dataArray.length(); i++) {
                        JSONObject trade = dataArray.getJSONObject(i);
                        TradeItem item = new TradeItem(
                                trade.getString("s"),
                                trade.getDouble("p"),
                                trade.getDouble("v"),
                                trade.getLong("t"),
                                TradeType.REALTIME
                        );
                        boolean added = tradeQueue.offer(item);
                        if (!added) {
                            System.err.println("[TradeTapeManager] Warning: tradeQueue full. Dropping item: " + item);
                        }

                        if (listener != null) listener.onTrade(item);
                    }
                }

                @Override public void onClose(int code, String reason, boolean remote) {
                    System.out.println("WebSocket closed: " + reason);
                }

                @Override public void onError(Exception e) {
                    System.err.println("[TradeTapeManager] WebSocket error: " + e.getMessage());
                }
            };

            client.connect();
        } catch (Exception e) {
            System.err.println("[TradeTapeManager] Failed to connect WebSocket: " + e.getMessage());
        }
    }

    /**
     * Gracefully closes WebSocket connection when market closes.
     */
    private void closeWebSocket() {
        if (client != null && client.isOpen()) {
            client.close();
        }
    }

    /**
     * Subscribes to all tickers identified from API.
     */
    private void subscribeToTopTickers() {
        try {
            for (String symbol : fetchTopTickersFromAPI()) {
                subscribe(symbol);
            }
        } catch (Exception e) {
            System.err.println("Error subscribing to top tickers: " + e.getMessage());
        }
    }

    /**
     * Switches to simulated trade feed by polling Alpha Vantage.
     * Runs in a loop on a background thread.
     */
    private void loadTopTickersFromAPI() {
        try {
            JSONObject response = fetchTopTickersJSON();
            JSONArray gainers = response.getJSONArray("top_gainers");
            JSONArray losers = response.getJSONArray("top_losers");
            JSONArray active = response.getJSONArray("most_actively_traded");

            new Thread(() -> {
                try {
                    while (true) {
                        long now = System.currentTimeMillis();

                        simulateTrades("Top Gainers", gainers, TradeType.GAINER, now);
                        simulateTrades("Top Losers", losers, TradeType.LOSER, now);
                        simulateTrades("Most Active", active, TradeType.ACTIVE, now);

                        Thread.sleep(800);  // Delay between each cycle
                    }
                } catch (Exception e) {
                    System.err.println("Error simulating after-hours loop: " + e.getMessage());
                }
            }).start();

        } catch (Exception e) {
            System.err.println("Failed to load fallback tickers: " + e.getMessage());
        }
    }

    /**
     * Simulates trade items based on Alpha Vantage data.
     */
    private void simulateTrades(String header, JSONArray data, TradeType type, long now) {
        if (listener != null) {
            listener.onTrade(new TradeItem(header, 0, 0, now, TradeType.HEADER));
        }

        for (int i = 0; i < data.length(); i++) {
            JSONObject obj = data.getJSONObject(i);
            TradeItem item = new TradeItem(
                    obj.getString("ticker"),
                    obj.optDouble("price", 0),
                    Double.parseDouble(obj.getString("change_percentage").replace("%", "").trim()),
                    now,
                    type
            );
            if (listener != null) listener.onTrade(item);
        }
    }

    /**
     * Fetches JSON data for top gainers, losers, and active stocks from Alpha Vantage.
     */
    private JSONObject fetchTopTickersJSON() throws Exception {
        Dotenv dotenv = Dotenv.load();
        String apiKey = dotenv.get("ALPHA_API_KEY");
        String url = "https://www.alphavantage.co/query?function=TOP_GAINERS_LOSERS&apikey=" + apiKey;

        Scanner scanner = new Scanner(new URL(url).openStream());
        StringBuilder json = new StringBuilder();
        while (scanner.hasNext()) json.append(scanner.nextLine());
        scanner.close();

        return new JSONObject(json.toString());
    }

    /**
     * Extracts top 7 tickers from each category (gainers, losers, active).
     */
    private Set<String> fetchTopTickersFromAPI() throws Exception {
        JSONObject response = fetchTopTickersJSON();
        JSONArray gainers = response.getJSONArray("top_gainers");
        JSONArray losers = response.getJSONArray("top_losers");
        JSONArray active = response.getJSONArray("most_actively_traded");

        Set<String> uniqueTickers = new HashSet<>();
        for (int i = 0; i < Math.min(7, gainers.length()); i++)
            uniqueTickers.add(gainers.getJSONObject(i).getString("ticker"));
        for (int i = 0; i < Math.min(7, losers.length()); i++)
            uniqueTickers.add(losers.getJSONObject(i).getString("ticker"));
        for (int i = 0; i < Math.min(14, active.length()); i++)
            uniqueTickers.add(active.getJSONObject(i).getString("ticker"));

        return uniqueTickers;
    }

    /**
     * Sends a subscription request to the WebSocket.
     */
    public void subscribe(String symbol) {
        if (client != null && client.isOpen()) {
            client.send("{\"type\":\"subscribe\",\"symbol\":\"" + symbol + "\"}");
        }
    }

    /**
     * Returns a color-coded value based on the trade type and comparison to previous close.
     */
    public Color getTradeColor(TradeItem item) {
        return switch (item.type) {
            case GAINER -> Color.GREEN;
            case LOSER -> Color.RED;
            case HEADER, ACTIVE -> Color.WHITE;
            case REALTIME -> {
                double previousClose = getPreviousClose(item.symbol);
                if (previousClose < 0) yield Color.WHITE;
                if (item.price > previousClose) yield Color.GREEN;
                if (item.price < previousClose) yield Color.RED;
                yield Color.WHITE;
            }
        };
    }

    /**
     * Caches and returns the previous close price for a symbol.
     */
    private double getPreviousClose(String symbol) {
        if (!previousCloseCache.containsKey(symbol)) {
            double close = fetchPreviousClose(symbol);
            if (close > 0) previousCloseCache.put(symbol, close);
        }
        return previousCloseCache.getOrDefault(symbol, -1.0);
    }

    /**
     * Fetches the previous close value from Alpha Vantage API.
     */
    private double fetchPreviousClose(String symbol) {
        try {
            Dotenv dotenv = Dotenv.load();
            String apiKey = dotenv.get("ALPHA_API_KEY");
            String url = "https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol=" + symbol + "&apikey=" + apiKey;

            Scanner scanner = new Scanner(new URL(url).openStream());
            StringBuilder json = new StringBuilder();
            while (scanner.hasNext()) json.append(scanner.nextLine());
            scanner.close();

            JSONObject response = new JSONObject(json.toString());
            JSONObject quote = response.getJSONObject("Global Quote");
            String closeStr = quote.getString("08. previous close");
            return Double.parseDouble(closeStr);

        } catch (Exception e) {
            System.err.println("Failed to fetch previous close for " + symbol + ": " + e.getMessage());
            return -1.0;
        }
    }

    /**
     * Data structure representing a single trade item for the tape.
     */
    public record TradeItem(String symbol, double price, double volume, long timestamp, TradeType type) {
    }
}
