package com.minibloomberg.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class WatchlistPanel extends JPanel {
    private final JPanel listPanel;
    private final Map<String, JLabel> tickerLabels = new ConcurrentHashMap<>();
    private final Consumer<String> onTickerSelected;

    public WatchlistPanel(Consumer<String> onTickerSelected) {
        this.onTickerSelected = onTickerSelected;

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(150, 0));
        setBackground(ColorPalette.EERIE_BLACK);

        JLabel title = new JLabel(" Watchlist");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setForeground(ColorPalette.ANTI_FLASH_WHITE);
        title.setOpaque(true);
        title.setBackground(ColorPalette.EERIE_BLACK);
        title.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        title.setHorizontalAlignment(SwingConstants.CENTER);
        add(title, BorderLayout.NORTH);

        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(ColorPalette.EERIE_BLACK);
        add(listPanel, BorderLayout.CENTER);
    }

    /**
     * Updates the displayed price and change for a ticker. Adds it if not already present.
     */
    public void updateTicker(String ticker, double price, double changePercent) {
        SwingUtilities.invokeLater(() -> {
            JLabel label = tickerLabels.get(ticker);
            if (label == null) {
                label = createTickerLabel(ticker, price, changePercent);
                tickerLabels.put(ticker, label);
                listPanel.add(label);
                revalidate();
            } else {
                label.setText(formatTicker(ticker, price, changePercent));
            }
        });
    }

    /**
     * Creates a styled label for a ticker with hover effects and click handling.
     */
    private JLabel createTickerLabel(String ticker, double price, double changePercent) {
        JLabel label = new JLabel(formatTicker(ticker, price, changePercent));
        label.setOpaque(true);
        label.setBackground(ColorPalette.EERIE_BLACK);
        label.setForeground(ColorPalette.ANTI_FLASH_WHITE);
        label.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        label.setFont(new Font("SansSerif", Font.PLAIN, 14));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        label.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                label.setBackground(ColorPalette.JET);
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                label.setBackground(ColorPalette.EERIE_BLACK);
            }
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                onTickerSelected.accept(ticker);
            }
        });

        return label;
    }

    /**
     * Formats the ticker label with HTML for color and arrow indicators.
     */
    private String formatTicker(String ticker, double price, double changePercent) {
        String arrow = changePercent > 0 ? "▲" : changePercent < 0 ? "▼" : "";
        String color = changePercent > 0 ? ColorPalette.HEX_GREEN
                      : changePercent < 0 ? ColorPalette.HEX_RED
                      : ColorPalette.HEX_WHITE;

        return String.format(
            "<html><b style='color:%s;'>%s</b> $%.2f <font color='%s'>%s%.2f%%</font></html>",
            ColorPalette.HEX_WHITE, ticker, price, color, arrow, Math.abs(changePercent)
        );
    }

    /**
     * Removes a ticker from the watchlist display.
     */
    public void removeTicker(String ticker) {
        SwingUtilities.invokeLater(() -> {
            JLabel label = tickerLabels.remove(ticker);
            if (label != null) {
                listPanel.remove(label);
                listPanel.revalidate();
                listPanel.repaint();
            }
        });
    }
}
