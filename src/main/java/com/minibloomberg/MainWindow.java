package com.minibloomberg;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.minibloomberg.logic.LivePriceManager;
import com.minibloomberg.logic.TradeTapeManager;
import com.minibloomberg.ui.NewsPanel;
import com.minibloomberg.ui.TradeTapePanel;
import com.minibloomberg.ui.WatchlistPanel;


public class MainWindow extends JFrame {

    public MainWindow() {
        setTitle("Katz Terminal");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel();
        topPanel.setBackground(new Color(0x252525)); 
        topPanel.setPreferredSize(new Dimension(0, 60));
        add(topPanel, BorderLayout.NORTH);

        WatchlistPanel watchlistPanel = new WatchlistPanel(ticker -> {
            System.out.println("Clicked: " + ticker);
        });
        watchlistPanel.setBackground(new Color(0x252525)); 
        watchlistPanel.setPreferredSize(new Dimension(225, 0));
        add(watchlistPanel, BorderLayout.WEST);

        LivePriceManager livePriceManager = new LivePriceManager(watchlistPanel);
        String[] tickers = {"AAPL", "TSLA", "GOOGL", "MSFT", "NVDA"};
        for (String symbol : tickers) {
            livePriceManager.addTicker(symbol);
        }
        livePriceManager.connect(); 

        JPanel centerPanel = new JPanel();
        centerPanel.setBackground(new Color(0x121212));
        add(centerPanel, BorderLayout.CENTER);

        NewsPanel newsPanel = new NewsPanel();
        newsPanel.setPreferredSize(new Dimension(350, 0));
        add(newsPanel, BorderLayout.EAST);
        

        TradeTapeManager manager = new TradeTapeManager();
        TradeTapePanel tapePanel = new TradeTapePanel(manager);
        manager.setTradeListener(tapePanel::displayTrade);
        manager.connect();
        tapePanel.setAfterHoursMode(!manager.isMarketOpen());

        add(tapePanel, BorderLayout.SOUTH); 


        getContentPane().setBackground(new Color(0x1e1e1e)); 

        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainWindow());
    }
}
