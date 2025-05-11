package com.minibloomberg;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.minibloomberg.logic.LivePriceManager;
import com.minibloomberg.logic.TradeTapeManager;
import com.minibloomberg.logic.TradeTapeManager.TradeItem;
import com.minibloomberg.ui.ComponentFactory;
import com.minibloomberg.ui.FadeTransitionPanel;
import com.minibloomberg.ui.NewsPanel;
import com.minibloomberg.ui.SearchController;
import com.minibloomberg.ui.TickerDetailPanel;
import com.minibloomberg.ui.TradeTapePanel;
import com.minibloomberg.ui.WatchlistPanel;

public class MainWindow extends JFrame {
    private final SearchController searchController;

    public MainWindow() {
        setTitle("Katz Terminal");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1300, 900));
        setLayout(new BorderLayout());

        // Create top search bar
        JPanel topPanel = ComponentFactory.createTopPanel();
        JTextField searchField = ComponentFactory.getSearchField();
        JButton searchButton = ComponentFactory.getSearchButton();

        topPanel.add(searchField);
        topPanel.add(searchButton);
        add(topPanel, BorderLayout.NORTH);

        // Create watchlist + live manager
        WatchlistPanel watchlistPanel = new WatchlistPanel(this::searchTicker);
        LivePriceManager livePriceManager = new LivePriceManager(watchlistPanel);
        livePriceManager.connect();
        watchlistPanel.setBackground(new Color(26, 26, 26));
        watchlistPanel.setPreferredSize(new Dimension(225, 0));
        add(watchlistPanel, BorderLayout.WEST);

        // Center panel setup
        FadeTransitionPanel centerContainer = new FadeTransitionPanel();
        centerContainer.setBackground(new Color(26, 26, 26));
        add(centerContainer, BorderLayout.CENTER);

        NewsPanel newsPanel = new NewsPanel();
        newsPanel.setPreferredSize(new Dimension(350, 0));
        add(newsPanel, BorderLayout.EAST);


        TradeTapePanel tapePanel = getTradeTapePanel();

        add(tapePanel, BorderLayout.SOUTH); 
        TickerDetailPanel[] tickerDetailPanelHolder = new TickerDetailPanel[1];
        centerContainer.showWithFade(ComponentFactory.getEmptyPlaceholder());

        // Create controller
        searchController = new SearchController(this, centerContainer, tickerDetailPanelHolder, livePriceManager);

        // Search actions
        searchButton.addActionListener(e -> {
            String query = searchField.getText();
            searchTicker(query);
            searchField.setText("");
        });

        searchField.addActionListener(e -> searchButton.doClick());

        getContentPane().setBackground(new Color(0x1e1e1e));
        setVisible(true);
    }

    private static TradeTapePanel getTradeTapePanel() {
        TradeTapeManager manager = new TradeTapeManager();
        TradeTapePanel tapePanel = new TradeTapePanel(manager);
        manager.setTradeListener(new TradeTapeManager.TradeListener() {
            @Override
            public void onTrade(TradeItem trade) {
                tapePanel.displayTrade(trade);
            }

            @Override
            public void onMarketModeChanged(boolean isAfterHours) {
                tapePanel.setAfterHoursMode(isAfterHours);
            }
        });
        manager.connect();
        tapePanel.setAfterHoursMode(!manager.isMarketOpen());
        return tapePanel;
    }

    private void searchTicker(String ticker) {
        searchController.search(ticker);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MainWindow::new);
    }
}
