package com.minibloomberg.logic;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONObject;

import com.minibloomberg.data.TradeData;
import com.minibloomberg.ui.WatchlistPanel;

import io.github.cdimascio.dotenv.Dotenv;

public class LivePriceManager {
    private final Map<String, TradeData> tickerData = new ConcurrentHashMap<>();
    private final Map<String, Double> basePrices = new ConcurrentHashMap<>();
    private final WatchlistPanel watchlistPanel;
    private WebSocketClient client;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public LivePriceManager(WatchlistPanel panel) {
        this.watchlistPanel = panel;
    }

    public void connect() {
        Dotenv dotenv = Dotenv.load();
        String apiKey = dotenv.get("FINNHUB_API_KEY");

        try {
            client = new WebSocketClient(new URI("wss://ws.finnhub.io?token=" + apiKey)) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    System.out.println("Connected to Finnhub");
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

                        double base = basePrices.get(symbol);
                        double change = ((price - base) / base) * 100.0;

                        tickerData.put(symbol, new TradeData(price, change));
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("WebSocket closed: " + reason);
                }

                @Override
                public void onError(Exception ex) {
                    ex.printStackTrace();
                }
            };

            client.connect();

        } catch (Exception e) {
            e.printStackTrace();
        }


        executor.scheduleAtFixedRate(() -> {
            for (Map.Entry<String, TradeData> entry : tickerData.entrySet()) {
                String symbol = entry.getKey();
                TradeData trade = entry.getValue();
                watchlistPanel.updateTicker(symbol, trade.getPrice(), trade.getChangePercent());
            }
        }, 0, 1, TimeUnit.SECONDS);

        executor.scheduleAtFixedRate(() -> {
            if (client != null && client.isOpen()) {
                client.send("ping");
            }
        }, 30, 30, TimeUnit.SECONDS); 
        
    }

    public void addTicker(String symbol){
        addTicker(symbol,1.00);
    }
    public void addTicker(String symbol, double previousClose) {
        tickerData.putIfAbsent(symbol, new TradeData(0, 0));
        basePrices.putIfAbsent(symbol, previousClose);
        if (client != null && client.isOpen()) {
            client.send("{\"type\":\"subscribe\",\"symbol\":\"" + symbol + "\"}");
        }
    }
    
    public void removeTicker(String symbol) {
        tickerData.remove(symbol);
        basePrices.remove(symbol);
    
        if (client != null && client.isOpen()) {
            client.send("{\"type\":\"unsubscribe\",\"symbol\":\"" + symbol + "\"}");
        }

        watchlistPanel.removeTicker(symbol);
    }

    public boolean containsTicker(String symbol) {
        return tickerData.containsKey(symbol);
    }
}
