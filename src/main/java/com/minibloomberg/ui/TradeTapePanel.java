package com.minibloomberg.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

import com.minibloomberg.logic.TradeTapeManager;
import com.minibloomberg.logic.TradeTapeManager.TradeItem;

public class TradeTapePanel extends JPanel {
    private final List<JLabel> tradeLabels = new ArrayList<>();
    private final TradeTapeManager tradeManager;
    private final JLabel afterHoursBanner = new JLabel("AFTER HOURS", SwingConstants.LEFT);

    private static final int LABEL_SPACING = 50;

    public TradeTapePanel(TradeTapeManager tradeManager) {
        this.tradeManager = tradeManager;

        setLayout(null);
        setBackground(ColorPalette.EERIE_BLACK);  // Updated from raw color
        setPreferredSize(new Dimension(0, 50));

        afterHoursBanner.setFont(new Font("Monospaced", Font.BOLD, 13));
        afterHoursBanner.setForeground(ColorPalette.ORANGE_PEEL);  // Updated from Color.ORANGE
        afterHoursBanner.setBounds(10, 0, 350, 20);
        add(afterHoursBanner);
        afterHoursBanner.setVisible(false);

        Timer animationTimer = new Timer(30, e -> onTick());
        animationTimer.start();
    }

    private void onTick() {
        int speed = 1;
        Iterator<JLabel> it = tradeLabels.iterator();
        while (it.hasNext()) {
            JLabel label = it.next();
            Point pos = label.getLocation();
            label.setLocation(pos.x - speed, pos.y);

            if (label.getX() + label.getWidth() < 0) {
                remove(label);
                it.remove();
            }
        }
        repaint();
    }

    private int getRightmostX() {
        int rightmost = 0;
        for (JLabel label : tradeLabels) {
            int rightEdge = label.getX() + label.getWidth();
            if (rightEdge > rightmost) rightmost = rightEdge;
        }
        return rightmost == 0 ? getWidth() : rightmost;
    }

    public void displayTrade(TradeItem trade) {
        SwingUtilities.invokeLater(() -> {
            String text = formatTradeText(trade);
            Color color = tradeManager.getTradeColor(trade);
            JLabel tradeLabel = createTradeLabel(text, color);

            int spawnX = getRightmostX() + LABEL_SPACING;
            int y = (getHeight() - tradeLabel.getPreferredSize().height) / 2;
            tradeLabel.setLocation(spawnX, y);

            add(tradeLabel);
            tradeLabels.add(tradeLabel);
            repaint();
        });
    }

    private String formatTradeText(TradeItem trade) {
        return switch (trade.type()) {
            case HEADER -> trade.symbol();
            case GAINER, LOSER -> String.format("%s $%.2f (%.1f%%)", trade.symbol(), trade.price(), trade.volume());
            case ACTIVE -> String.format("%s %.1fM", trade.symbol(), trade.volume() / 1_000_000.0);
            case REALTIME -> String.format("%s $%.2f %.0f", trade.symbol(), trade.price(), trade.volume());
        };
    }

    private JLabel createTradeLabel(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Monospaced", Font.PLAIN, 14));
        if (!tradeManager.isMarketOpen()) {
            label.setBorder(new EmptyBorder(12, 0, 0, 0));
        }
        label.setForeground(color);
        label.setSize(label.getPreferredSize());
        label.setVerticalAlignment(SwingConstants.CENTER);
        return label;
    }

    public void setAfterHoursMode(boolean isAfterHours) {
        SwingUtilities.invokeLater(() -> afterHoursBanner.setVisible(isAfterHours));
    }
}
