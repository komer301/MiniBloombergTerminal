package com.minibloomberg;

import java.awt.*;
import javax.swing.*;
import com.minibloomberg.logic.LivePriceManager;
import com.minibloomberg.logic.Stock;
import com.minibloomberg.ui.TickerDetailPanel;
import com.minibloomberg.ui.WatchlistPanel;

public class MainWindow extends JFrame {
    LivePriceManager livePriceManager;
    private JPanel centerContainer;
    private TickerDetailPanel[] tickerDetailPanelHolder;

    public MainWindow() {
        setTitle("Mini Bloomberg Terminal");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1200, 800));
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
        topPanel.setBackground(new Color(0x252525));
        topPanel.setPreferredSize(new Dimension(0, 80));

        JTextField searchField = new JTextField();
        searchField.setPreferredSize(new Dimension(300, 35));
        searchField.setFont(new Font("Consolas", Font.PLAIN, 16));
        searchField.setBackground(new Color(0x1e1e1e));
        searchField.setForeground(Color.YELLOW);
        searchField.setCaretColor(Color.YELLOW);
        searchField.setBorder(BorderFactory.createLineBorder(Color.YELLOW));

        JButton searchButton = new JButton("Search");
        searchButton.setPreferredSize(new Dimension(50, 25));
        searchButton.setForeground(Color.YELLOW);
        searchButton.setBackground(new Color(0x1e1e1e));
        searchButton.setBorder(BorderFactory.createLineBorder(Color.YELLOW));
        searchButton.setFont(new Font("Consolas", Font.BOLD, 14));
        searchButton.setBorderPainted(false);
        searchButton.setFocusPainted(false);
        searchButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        topPanel.add(searchField);
        topPanel.add(searchButton);
        add(topPanel, BorderLayout.NORTH);

//        WatchlistPanel watchlistPanel = new WatchlistPanel(ticker -> System.out.println("Clicked: " + ticker));
        WatchlistPanel watchlistPanel = new WatchlistPanel(ticker ->
                searchForTicker(ticker, centerContainer, tickerDetailPanelHolder));
        watchlistPanel.setBackground(new Color(0x252525));
        watchlistPanel.setPreferredSize(new Dimension(225, 0));
        add(watchlistPanel, BorderLayout.WEST);

        livePriceManager = new LivePriceManager(watchlistPanel);
        livePriceManager.connect();

        centerContainer = new JPanel(new BorderLayout());
        centerContainer.setBackground(new Color(0x000000));
        add(centerContainer, BorderLayout.CENTER);

        tickerDetailPanelHolder = new TickerDetailPanel[1];
        tickerDetailPanelHolder[0] = new TickerDetailPanel("AAPL", livePriceManager);
        centerContainer.add(tickerDetailPanelHolder[0], BorderLayout.CENTER);

        searchButton.addActionListener(e -> {
            String storedTicker = searchField.getText();
            searchForTicker(storedTicker, centerContainer, tickerDetailPanelHolder);
        });

        searchField.addActionListener(e -> searchButton.doClick());

        JPanel rightPanel = new JPanel();
        rightPanel.setBackground(new Color(0x252525));
        rightPanel.setPreferredSize(new Dimension(250, 0));
        add(rightPanel, BorderLayout.EAST);

        getContentPane().setBackground(new Color(0x1e1e1e)); 

        setVisible(true);
    }

//    private void searchForTicker(String ticker, JPanel centerContainer, TickerDetailPanel[] tickerDetailPanelHolder) {
//        ticker = ticker.toUpperCase().trim();
//        if (!ticker.isEmpty()) {
//            Stock snapshot = com.minibloomberg.logic.StockDataFetcher.fetchStockSnapshot(ticker);
//
//            if (snapshot == null) {
//                showInvalidTickerPopup(centerContainer, tickerDetailPanelHolder);
//            } else {
//                centerContainer.remove(tickerDetailPanelHolder[0]);
//                tickerDetailPanelHolder[0] = new TickerDetailPanel(ticker, livePriceManager);
//                centerContainer.add(tickerDetailPanelHolder[0], BorderLayout.CENTER);
//                centerContainer.revalidate();
//                centerContainer.repaint();
//            }
//        }
//    }

    private void searchForTicker(String ticker, JPanel centerContainer, TickerDetailPanel[] tickerDetailPanelHolder) {
        ticker = ticker.toUpperCase().trim();
        if (!ticker.isEmpty()) {
            String currentTicker = tickerDetailPanelHolder[0] != null ?
                    tickerDetailPanelHolder[0].getCurrentTicker() : null;

            if (ticker.equalsIgnoreCase(currentTicker)) {
                showAlreadyViewingPopup(ticker);
                return;
            }

            Stock snapshot = com.minibloomberg.logic.StockDataFetcher.fetchStockSnapshot(ticker);

            if (snapshot == null) {
                showInvalidTickerPopup(centerContainer, tickerDetailPanelHolder);
            } else {
                centerContainer.remove(tickerDetailPanelHolder[0]);
                tickerDetailPanelHolder[0] = new TickerDetailPanel(ticker, livePriceManager);
                centerContainer.add(tickerDetailPanelHolder[0], BorderLayout.CENTER);
                centerContainer.revalidate();
                centerContainer.repaint();
            }
        }
    }

    private void showAlreadyViewingPopup(String ticker) {
        JWindow popup = new JWindow();
        popup.setBackground(new Color(0, 0, 0, 0));

        JLabel message = new JLabel("Already viewing " + ticker, SwingConstants.CENTER);
        message.setOpaque(true);
        message.setBackground(new Color(30, 30, 30));
        message.setForeground(Color.YELLOW);
        message.setFont(new Font("Consolas", Font.BOLD, 14));
        message.setBorder(BorderFactory.createLineBorder(Color.YELLOW));

        popup.getContentPane().add(message);
        popup.setSize(220, 40);

        Point location = getLocationOnScreen();
        popup.setLocation(location.x + (getWidth() - popup.getWidth()) / 2, location.y + 100);

        popup.setVisible(true);

        Timer timer = new Timer(1500, e -> popup.dispose());
        timer.setRepeats(false);
        timer.start();
    }

    private void showInvalidTickerPopup(JPanel centerContainer, TickerDetailPanel[] tickerDetailPanelHolder) {
        JDialog dialog = new JDialog(this, "Invalid Ticker", true);
        dialog.setSize(400, 150);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(new Color(0x1e1e1e));

        JLabel message = new JLabel("Enter a valid ticker to display data.", SwingConstants.CENTER);
        message.setFont(new Font("Consolas", Font.BOLD, 16));
        message.setForeground(Color.RED);
        dialog.add(message, BorderLayout.CENTER);

        JButton closeButton = new JButton("Close");
        closeButton.setForeground(Color.RED);
        closeButton.setBackground(Color.BLACK);
        closeButton.setFont(new Font("Consolas", Font.BOLD, 14));
        closeButton.setBorderPainted(false);
        closeButton.setContentAreaFilled(false);
        closeButton.setFocusPainted(false);
        closeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        closeButton.addActionListener(e -> {
            dialog.dispose();
            resetToDefault(centerContainer, tickerDetailPanelHolder, livePriceManager);
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(new Color(0x1e1e1e));
        buttonPanel.add(closeButton);

        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                dialog.dispose();
                resetToDefault(centerContainer, tickerDetailPanelHolder, livePriceManager);
            }
        });

        dialog.setVisible(true);
    }

    private void resetToDefault(JPanel centerContainer, TickerDetailPanel[] tickerDetailPanelHolder,
                                LivePriceManager livePriceManager) {
        centerContainer.remove(tickerDetailPanelHolder[0]);
        tickerDetailPanelHolder[0] = new TickerDetailPanel("AAPL", livePriceManager);
        centerContainer.add(tickerDetailPanelHolder[0], BorderLayout.CENTER);
        centerContainer.revalidate();
        centerContainer.repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MainWindow::new);
    }
}
