package com.minibloomberg.ui;

import com.minibloomberg.data.HistoricalData;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class ChartPanel extends JPanel {
    private HistoricalData data;

    public ChartPanel() {
        setBackground(Color.BLACK);
    }

    public void setHistoricalData(HistoricalData data) {
        this.data = data;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (data == null || data.timestamps.isEmpty()) {
            return;
        }

        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(Color.GREEN);

        int width = getWidth();
        int height = getHeight();

        List<Double> prices = data.closePrices;
        double minPrice = prices.stream().min(Double::compareTo).orElse(0.0);
        double maxPrice = prices.stream().max(Double::compareTo).orElse(1.0);

        int margin = 40;
        int n = prices.size();

        for (int i = 1; i < n; i++) {
            int x1 = margin + (i - 1) * (width - 2 * margin) / (n - 1);
            int x2 = margin + i * (width - 2 * margin) / (n - 1);

            int y1 = height - margin - (int) ((prices.get(i - 1) - minPrice) / (maxPrice - minPrice) * (height - 2 * margin));
            int y2 = height - margin - (int) ((prices.get(i) - minPrice) / (maxPrice - minPrice) * (height - 2 * margin));

            g2d.drawLine(x1, y1, x2, y2);
        }
    }
}
