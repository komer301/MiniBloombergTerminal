//package com.minibloomberg.ui;
//
//import com.minibloomberg.logic.Stock;
//
//import javax.swing.*;
//import javax.swing.border.LineBorder;
//import java.awt.*;
//import java.awt.event.ComponentAdapter;
//import java.awt.event.ComponentEvent;
//
//public class TickerDetailPanel extends JPanel {
//
//    public TickerDetailPanel() {
//        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
//        setBackground(new Color(0xCFCCCC));
//
//        // Stock Info
//        JPanel infoContainer = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
//        infoContainer.setOpaque(false);
//
//        JPanel infoPanel = new JPanel();
//        infoPanel.setBackground(new Color(0xFFFFFF));
//        infoPanel.setLayout(new GridLayout(4, 2, 10, 10));
//        infoContainer.setBorder(BorderFactory.createEmptyBorder(25, 0, 25, 0));
//        infoContainer.add(infoPanel);
//
//        Stock snapshot = com.minibloomberg.logic.StockDataFetcher.fetchStockSnapshot("TSLA");
//        assert snapshot != null;
//        infoPanel.add(new JLabel("Symbol: " + snapshot.symbol));
//        infoPanel.add(new JLabel("Company: " + snapshot.companyName));
//        infoPanel.add(new JLabel("Change: " + snapshot.change));
//        infoPanel.add(new JLabel("Percent Change: " + snapshot.percentChange));
//        infoPanel.add(new JLabel("Previous Close: " + snapshot.previousClose));
//        infoPanel.add(new JLabel("Current Price: " + snapshot.currentPrice));
//        infoPanel.add(new JLabel("Day Low: " + snapshot.dayLow));
//        infoPanel.add(new JLabel("Day High: " + snapshot.dayHigh));
//        infoPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
////        add(infoPanel);
//
//        add(infoContainer);
//        add(Box.createVerticalStrut(15));
//
//        // Chart
//        JPanel chartContainer = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
////        chartContainer.setBorder(BorderFactory.createEmptyBorder(50, 0, 0, 0));
//        chartContainer.setOpaque(false);
//
//        JPanel chartPanel = new JPanel();
//        chartPanel.setBackground(Color.BLACK);
//
//        chartPanel.setBorder(BorderFactory.createCompoundBorder(
//                new LineBorder(new Color(0xCCCCCC), 2 , true),
//                BorderFactory.createEmptyBorder(15, 15, 15, 15)
//        ));
//        chartContainer.add(chartPanel);
//
//        this.addComponentListener(new ComponentAdapter() {
//            @Override
//            public void componentResized(ComponentEvent e) {
//                int parentWidth = getWidth();
//                int parentHeight = getHeight();
//
//                int newWidth = (int) (0.8 * parentWidth);
//                int newHeight = Math.max(400, (int) (0.45 * parentHeight));
//                int infoHeight = 150;
//
//                chartPanel.setPreferredSize(new Dimension(newWidth, newHeight));
//                chartPanel.revalidate();
//
//                infoPanel.setPreferredSize(new Dimension(newWidth, infoHeight));
//                infoPanel.revalidate();
//
//                int verticalSpace = parentHeight - newHeight - infoHeight - 200;
//                int padding = Math.max(verticalSpace / 2, 20);
//
//                infoContainer.setBorder(BorderFactory.createEmptyBorder(padding, 0, padding, 0));
//                infoContainer.revalidate();
//            }
//        });
//        add(chartContainer);
//
//
//        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
//        JButton addToWatchlist = new JButton("Add to Watchlist");
//        buttonPanel.setOpaque(false);
//        addToWatchlist.setPreferredSize(new Dimension(220, 45));
//        addToWatchlist.setFont(new Font("SansSerif", Font.BOLD, 14));
//        buttonPanel.add(addToWatchlist);
//        buttonPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
//        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 50, 0));
//        add(buttonPanel);
//    }
//}

package com.minibloomberg.ui;

import com.minibloomberg.logic.LivePriceManager;
import com.minibloomberg.logic.Stock;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class TickerDetailPanel extends JPanel {

    private static final Font TERMINAL_FONT = new Font("Consolas", Font.PLAIN, 14);
    private static final Color BG_DARK = new Color(0x0F0F0F);
    private static final Color TEXT_NEUTRAL = new Color(0xCCCCCC);
    private static final Color TEXT_GAIN = new Color(0x00FF00);
    private static final Color TEXT_LOSS = new Color(0xFF4D4D);
    private static final Color TEXT_PRIMARY = Color.YELLOW;

    private final LivePriceManager livePriceManager;
    private final String currentTicker;
    private double previousClosePrice;
    private JButton addToWatchlist;

    public TickerDetailPanel(String ticker, LivePriceManager manager) {
        this.livePriceManager = manager;
        this.currentTicker = ticker;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(BG_DARK);

        JPanel infoContainer = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        infoContainer.setOpaque(false);

        JPanel infoPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        infoPanel.setBackground(BG_DARK);
        infoContainer.add(infoPanel);

        Stock snapshot = com.minibloomberg.logic.StockDataFetcher.fetchStockSnapshot(ticker);

        if (snapshot == null) {
            JLabel errorLabel = new JLabel("Enter a valid ticker to display data.");
            errorLabel.setForeground(Color.RED);
            errorLabel.setFont(new Font("Consolas", Font.BOLD, 16));
            add(errorLabel);
            return;
        }

        previousClosePrice = snapshot.previousClose;

        Color changeColor = snapshot.change >= 0 ? TEXT_GAIN : TEXT_LOSS;

        infoPanel.add(createLabel("Symbol: " + snapshot.symbol, TEXT_PRIMARY));
        infoPanel.add(createLabel("Company: " + snapshot.companyName, TEXT_NEUTRAL));
        infoPanel.add(createLabel("Change: " + snapshot.change, changeColor));
        infoPanel.add(createLabel("Percent Change: " + snapshot.percentChange + "%", changeColor));
        infoPanel.add(createLabel("Previous Close: " + snapshot.previousClose, TEXT_NEUTRAL));
        infoPanel.add(createLabel("Current Price: " + snapshot.currentPrice, TEXT_NEUTRAL));
        infoPanel.add(createLabel("Day Low: " + snapshot.dayLow, TEXT_NEUTRAL));
        infoPanel.add(createLabel("Day High: " + snapshot.dayHigh, TEXT_NEUTRAL));

        infoPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        add(infoContainer);
        add(Box.createVerticalStrut(15));

        JPanel chartContainer = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        chartContainer.setOpaque(false);

        JPanel chartPanel = new JPanel();
        chartPanel.setBackground(Color.BLACK);
        chartPanel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(Color.WHITE, 2, true),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        chartContainer.add(chartPanel);

        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int parentWidth = getWidth();
                int parentHeight = getHeight();

                int newWidth = (int) (0.8 * parentWidth);
                int newHeight = Math.max(350, (int) (0.45 * parentHeight));
                int infoHeight = 150;

                chartPanel.setPreferredSize(new Dimension(newWidth, newHeight));
                chartPanel.revalidate();

                infoPanel.setPreferredSize(new Dimension(newWidth, infoHeight));
                infoPanel.revalidate();

                int verticalSpace = parentHeight - newHeight - infoHeight - 200;
                int padding = Math.max(verticalSpace / 2, 20);

                infoContainer.setBorder(BorderFactory.createEmptyBorder(padding, 0, padding, 0));
                infoContainer.revalidate();
            }
        });
        add(chartContainer);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setOpaque(false);

        addToWatchlist = new JButton("Add to Watchlist");
        addToWatchlist.setPreferredSize(new Dimension(220, 45));
        addToWatchlist.setFont(new Font("Consolas", Font.BOLD, 14));
        addToWatchlist.setForeground(TEXT_PRIMARY);
        addToWatchlist.setBackground(BG_DARK);
        addToWatchlist.setFocusPainted(false);
        addToWatchlist.setBorder(BorderFactory.createLineBorder(TEXT_PRIMARY));
        addToWatchlist.setCursor(new Cursor(Cursor.HAND_CURSOR));

        updateWatchlistButton();
        addToWatchlist.addActionListener(e -> handleWatchlistButtonClick());

        buttonPanel.add(addToWatchlist);
        buttonPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 50, 0));
        add(buttonPanel);
    }

    private JLabel createLabel(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(TERMINAL_FONT);
        label.setForeground(color);
        return label;
    }

    private void updateWatchlistButton() {
        if (livePriceManager.containsTicker(currentTicker)) {
            addToWatchlist.setText("Remove from Watchlist");
        } else {
            addToWatchlist.setText("Add to Watchlist");
        }
    }

    private void handleWatchlistButtonClick() {
        if (livePriceManager.containsTicker(currentTicker)) {
            livePriceManager.removeTicker(currentTicker);
        } else {
            livePriceManager.addTicker(currentTicker, previousClosePrice);
        }
        updateWatchlistButton();
    }
}
