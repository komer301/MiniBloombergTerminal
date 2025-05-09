package com.minibloomberg.ui;

import com.minibloomberg.data.HistoricalData;
import com.minibloomberg.logic.LivePriceManager;
import com.minibloomberg.data.Stock;
import com.minibloomberg.logic.StockDataFetcher;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;


public class TickerDetailPanel extends JPanel {

    private static final Font TERMINAL_FONT = new Font("Consolas", Font.PLAIN, 14);
    private static final Color BG_DARK = new Color(0x0F0F0F);
    private static final Color TEXT_NEUTRAL = new Color(0xCCCCCC);
    private static final Color TEXT_GAIN = new Color(0x00FF00);
    private static final Color TEXT_LOSS = new Color(0xFF4D4D);
    private static final Color TEXT_PRIMARY = Color.YELLOW;

    private final LivePriceManager livePriceManager;
    private final String currentTicker;
    private final Stock snapshot;
    private JButton addToWatchlist;

    private ChartPanel chartPanel;
    private final HistoricalData fullData;
    private JButton activeRangeButton;

    public TickerDetailPanel(String ticker, LivePriceManager manager) {
        this.livePriceManager = manager;
        this.currentTicker = ticker;
        this.snapshot = com.minibloomberg.logic.StockDataFetcher.fetchStockSnapshot(ticker);
        this.fullData =  StockDataFetcher.fetchHistoricalData(currentTicker);

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(BG_DARK);

        JPanel infoContainer = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        infoContainer.setOpaque(false);

        JPanel infoPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        infoPanel.setBackground(BG_DARK);
        infoContainer.add(infoPanel);

        if (snapshot == null) {
            JLabel errorLabel = new JLabel("Enter a valid ticker to display data.");
            errorLabel.setForeground(Color.RED);
            errorLabel.setFont(new Font("Consolas", Font.BOLD, 16));
            add(errorLabel);
            return;
        }

        Color changeColor = snapshot.change >= 0 ? TEXT_GAIN : TEXT_LOSS;

        infoPanel.add(createStyledLabel("Symbol:", snapshot.symbol, TEXT_PRIMARY));
        infoPanel.add(createStyledLabel("Company:", snapshot.companyName, TEXT_NEUTRAL));
        infoPanel.add(createStyledLabel("Change:", "$" + String.format("%.2f", snapshot.change), changeColor));
        infoPanel.add(createStyledLabel("Percent Change:", snapshot.percentChange + "%", changeColor));
        infoPanel.add(createStyledLabel("Previous Close:", "$" + snapshot.previousClose, TEXT_NEUTRAL));
        infoPanel.add(createStyledLabel("Current Price:", "$" + snapshot.currentPrice, TEXT_NEUTRAL));
        infoPanel.add(createStyledLabel("Day Low:", "$" + snapshot.dayLow, TEXT_NEUTRAL));
        infoPanel.add(createStyledLabel("Day High:", "$" + snapshot.dayHigh, TEXT_NEUTRAL));

        infoPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        add(infoContainer);
        add(Box.createVerticalStrut(15));

        JPanel chartContainer = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        chartContainer.setOpaque(false);

        JPanel innerPanel = new JPanel();
        innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.Y_AXIS));
        innerPanel.setOpaque(false);

        JPanel rangeButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        rangeButtonPanel.setOpaque(false);

        String[] ranges = {"All", "1Y", "6M", "3M", "1M", "1W", "3D"};
        for (String range : ranges) {
            JButton button = getjButton(range);

            rangeButtonPanel.add(button);

            if (range.equals("3D")) {
                activeRangeButton = button;
                highlightButton(activeRangeButton);
            }
        }

        innerPanel.add(rangeButtonPanel);
        innerPanel.add(Box.createVerticalStrut(10));

        chartPanel = new ChartPanel();
        chartPanel.setHistoricalData(fullData);
        chartPanel.setPreferredSize(new Dimension(800, 400));
        innerPanel.add(chartPanel);
        updateChartForRange("3D");

        chartContainer.add(innerPanel);

        add(chartContainer);

        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int parentWidth = getWidth();
                int parentHeight = getHeight();

                int newWidth = (int) (0.8 * parentWidth);
                int newHeight = Math.max(350, (int) (0.45 * parentHeight));
                int fixedInfoWidth = 500;
                int infoHeight = 150;

                chartPanel.setPreferredSize(new Dimension(newWidth, newHeight));
                chartPanel.revalidate();

                infoPanel.setPreferredSize(new Dimension(fixedInfoWidth, infoHeight));
                infoPanel.revalidate();

                int verticalSpace = parentHeight - newHeight - infoHeight - 200;
                int padding = Math.max(verticalSpace / 2, 20);

                infoContainer.setBorder(BorderFactory.createEmptyBorder(padding, 0, padding, 0));
                infoContainer.revalidate();
            }
        });

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

    private JButton getjButton(String range) {
        JButton button = new JButton(range);
        button.setFocusPainted(false);
        button.setBackground(Color.BLACK);
        button.setForeground(Color.YELLOW);
        button.setFont(new Font("Consolas", Font.BOLD, 14));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createLineBorder(Color.YELLOW));

        button.setOpaque(true);
        button.setContentAreaFilled(true);

        button.addActionListener(e -> {
            updateChartForRange(range);
            updateActiveButton(button);
        });
        return button;
    }

    public String getCurrentTicker() {
        return snapshot != null ? snapshot.symbol : null;
    }

    private JLabel createStyledLabel(String label, String value, Color valueColor) {
        String labelHtml = String.format(
                "<html><span style='color:white;'>%s </span><span style='color:%s;'>%s</span></html>",
                label,
                toHex(valueColor),
                value
        );
        JLabel styledLabel = new JLabel(labelHtml);
        styledLabel.setFont(TERMINAL_FONT);
        return styledLabel;
    }

    private String toHex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
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
            livePriceManager.addTicker(snapshot);
        }
        updateWatchlistButton();
    }

    private void updateChartForRange(String selectedRange) {
        if (fullData == null) return;

        List<Long> timestamps = fullData.timestamps;
        List<Double> closePrices = fullData.closePrices;

        int daysBack = switch (selectedRange) {
            case "3D" -> 3;
            case "1W" -> 7;
            case "1M" -> 21;
            case "3M" -> 63;
            case "6M" -> 126;
            case "1Y" -> 252;
            default -> Integer.MAX_VALUE;
        };

        int start = Math.max(0, timestamps.size() - daysBack);

        List<Long> filteredTimestamps = timestamps.subList(start, timestamps.size());
        List<Double> filteredPrices = closePrices.subList(start, closePrices.size());

        chartPanel.setHistoricalData(new HistoricalData(filteredTimestamps, filteredPrices));
    }

    private void updateActiveButton(JButton newActive) {
        if (activeRangeButton != null) {
            resetButtonStyle(activeRangeButton);
        }
        activeRangeButton = newActive;
        highlightButton(activeRangeButton);
    }

    private void highlightButton(JButton button) {
        button.setBackground(Color.YELLOW);
        button.setForeground(Color.BLACK);
    }

    private void resetButtonStyle(JButton button) {
        button.setBackground(Color.BLACK);
        button.setForeground(Color.YELLOW);
    }
}
