package com.minibloomberg;

import java.awt.*;
import javax.swing.*;

import com.minibloomberg.logic.LivePriceManager;
import com.minibloomberg.ui.*;

public class MainWindow extends JFrame {
    private final SearchController searchController;

    public MainWindow() {
        setTitle("Mini Bloomberg Terminal");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1200, 900));
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
        watchlistPanel.setBackground(new Color(0x252525));
        watchlistPanel.setPreferredSize(new Dimension(225, 0));
        add(watchlistPanel, BorderLayout.WEST);

        // Center panel setup
        FadeTransitionPanel centerContainer = new FadeTransitionPanel();
        centerContainer.setBackground(Color.BLACK);
        add(centerContainer, BorderLayout.CENTER);

        // Right-side panel (empty)
        JPanel rightPanel = new JPanel();
        rightPanel.setBackground(new Color(0x252525));
        rightPanel.setPreferredSize(new Dimension(250, 0));
        add(rightPanel, BorderLayout.EAST);

        // Initial placeholder
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

    private void searchTicker(String ticker) {
        searchController.search(ticker);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MainWindow::new);
    }
}
