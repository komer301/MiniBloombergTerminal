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

public class TradeTapeManager {
    private WebSocketClient client;
    private final BlockingQueue<TradeItem> tradeQueue = new LinkedBlockingQueue<>();
    private final Map<String, Double> previousCloseCache = new HashMap<>();
    private TradeListener listener;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private boolean lastMarketStatus = isMarketOpen();

    public enum TradeType {
        REALTIME, GAINER, LOSER, ACTIVE, HEADER
    }

    public interface TradeListener {
        void onTrade(TradeItem trade);
        void onMarketModeChanged(boolean isAfterHours);
    }

    public void setTradeListener(TradeListener listener) {
        this.listener = listener;
    }

    public void connect() {
        if (isMarketOpen()) {
            connectLiveWebSocket();
        } else {
            loadTopTickersFromAPI();
        }
        startMarketStatusWatcher();
    }

    private void startMarketStatusWatcher() {
        scheduler.scheduleAtFixedRate(() -> {
            boolean currentStatus = isMarketOpen();
            if (currentStatus != lastMarketStatus) {
                lastMarketStatus = currentStatus;

                if (currentStatus) {
                    // Market just opened
                    if (client == null || !client.isOpen()) {
                        connectLiveWebSocket();
                    }
                    if (listener != null) listener.onMarketModeChanged(false);
                } else {
                    // Market just closed
                    closeWebSocket();
                    loadTopTickersFromAPI();
                    if (listener != null) listener.onMarketModeChanged(true);
                }
            }
        }, 0, 1, TimeUnit.MINUTES); 
    }

    public boolean isMarketOpen() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("America/New_York"));
        LocalTime currentTime = now.toLocalTime();
        return currentTime.isAfter(LocalTime.of(9, 30)) && currentTime.isBefore(LocalTime.of(16, ));
    }

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
                        tradeQueue.offer(item);
                        if (listener != null) listener.onTrade(item);
                    }
                }

                @Override public void onClose(int code, String reason, boolean remote) {
                    System.out.println("WebSocket closed: " + reason);
                }

                @Override public void onError(Exception ex) {
                    ex.printStackTrace();
                }
            };

            client.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void closeWebSocket() {
        if (client != null && client.isOpen()) {
            client.close();
        }
    }

    private void subscribeToTopTickers() {
        try {
            for (String symbol : fetchTopTickersFromAPI()) {
                subscribe(symbol);
            }
        } catch (Exception e) {
            System.err.println("Error subscribing to top tickers: " + e.getMessage());
        }
    }

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
                    }
                } catch (Exception e) {
                    System.err.println("Error simulating after-hours loop: " + e.getMessage());
                }
            }).start();

        } catch (Exception e) {
            System.err.println("Failed to load fallback tickers: " + e.getMessage());
        }
    }

    private void simulateTrades(String header, JSONArray data, TradeType type, long now) throws InterruptedException {
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
            Thread.sleep(800);
        }
    }

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

    public void subscribe(String symbol) {
        if (client != null && client.isOpen()) {
            client.send("{\"type\":\"subscribe\",\"symbol\":\"" + symbol + "\"}");
        }
    }

    public void unsubscribe(String symbol) {
        if (client != null && client.isOpen()) {
            client.send("{\"type\":\"unsubscribe\",\"symbol\":\"" + symbol + "\"}");
        }
    }

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

    private double getPreviousClose(String symbol) {
        if (!previousCloseCache.containsKey(symbol)) {
            double close = fetchPreviousClose(symbol);
            if (close > 0) previousCloseCache.put(symbol, close);
        }
        return previousCloseCache.getOrDefault(symbol, -1.0);
    }

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

    public static class TradeItem {
        public final String symbol;
        public final double price;
        public final double volume;
        public final long timestamp;
        public final TradeType type;

        public TradeItem(String symbol, double price, double volume, long timestamp, TradeType type) {
            this.symbol = symbol;
            this.price = price;
            this.volume = volume;
            this.timestamp = timestamp;
            this.type = type;
        }
    }
}
