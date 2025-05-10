package com.minibloomberg.ui;

import java.awt.BorderLayout;
import java.awt.Color;
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

    private static final Color BACKGROUND_COLOR = new Color(0x252525);
    private static final Color HOVER_COLOR = new Color(0x2f2f2f);
    private static final Color TEXT_COLOR = Color.WHITE;

    public WatchlistPanel(Consumer<String> onTickerSelected) {
        this.onTickerSelected = onTickerSelected;

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(150, 0));
        setBackground(BACKGROUND_COLOR);

        JLabel title = new JLabel(" Watchlist");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setForeground(TEXT_COLOR);
        title.setOpaque(true);
        title.setBackground(BACKGROUND_COLOR);
        title.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        title.setHorizontalAlignment(SwingConstants.CENTER);
        add(title, BorderLayout.NORTH);

        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(BACKGROUND_COLOR);
        add(listPanel, BorderLayout.CENTER);
    }

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

    private JLabel createTickerLabel(String ticker, double price, double changePercent) {
        JLabel label = new JLabel(formatTicker(ticker, price, changePercent));
        label.setOpaque(true);
        label.setBackground(BACKGROUND_COLOR);
        label.setForeground(TEXT_COLOR);
        label.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        label.setFont(new Font("SansSerif", Font.PLAIN, 14));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Hover effect
        label.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                label.setBackground(HOVER_COLOR);
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                label.setBackground(BACKGROUND_COLOR);
            }

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                onTickerSelected.accept(ticker);
            }
        });

        return label;
    }

    private String formatTicker(String ticker, double price, double changePercent) {
        String arrow = changePercent > 0 ? "▲" : changePercent < 0 ? "▼" : "";
        String color = changePercent > 0 ? "#00ff00" : changePercent < 0 ? "#ff4444" : "#ffffff";
        return String.format("<html><b style='color:white;'>%s</b> $%.2f <font color='%s'>%s%.2f%%</font></html>",
                ticker, price, color, arrow, Math.abs(changePercent));
    }

    public void removeTicker(String ticker) {
        SwingUtilities.invokeLater(() -> {
            JLabel label = tickerLabels.remove(ticker); // remove from the map
            if (label != null) {
                listPanel.remove(label);
                listPanel.revalidate();
                listPanel.repaint();
            }
        });
    }
}
