package com.minibloomberg.ui;

import com.minibloomberg.data.HistoricalData;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ChartPanel extends JPanel {
    private HistoricalData data;
    private Integer hoverX = null;

    public ChartPanel() {
        setBackground(Color.BLACK);

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                hoverX = e.getX();
                repaint();
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                hoverX = null;
                repaint();
            }
        });
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
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        List<Double> prices = data.closePrices;
        List<Long> timestamps = data.timestamps;

        double minPrice = prices.stream().min(Double::compareTo).orElse(0.0);
        double maxPrice = prices.stream().max(Double::compareTo).orElse(1.0);

        int margin = 70;
        int n = prices.size();

        // Draw Y-axis price labels
        int targetLabels = Math.max(4, height / 100);
        double priceRange = maxPrice - minPrice;
        double rawInterval = priceRange / (targetLabels - 1);
//        double interval = roundToNiceNumber(rawInterval);
        double interval = roundToNiceNumber(priceRange, height);

        double niceMin = Math.floor(minPrice / interval) * interval;
        double niceMax = Math.ceil(maxPrice / interval) * interval;

        g2d.setColor(Color.LIGHT_GRAY);
        g2d.setFont(new Font("Consolas", Font.PLAIN, 10));

        for (double p = niceMin; p <= niceMax; p += interval) {
            int y = margin + (int) ((maxPrice - p) / (maxPrice - minPrice) * (height - 2 * margin));
            g2d.drawString(String.format("%.2f", p), 5, y + 5);

            // Draw light horizontal gridlines (comment this out if you want no gridlines)
            g2d.setColor(new Color(200, 200, 200, 50));
            g2d.drawLine(margin, y, width - margin, y);
            g2d.setColor(Color.LIGHT_GRAY);
        }

        // Draw stock price lines (green for up, red for down)
        g2d.setStroke(new BasicStroke(2));
        for (int i = 1; i < n; i++) {
            int x1 = margin + (i - 1) * (width - 2 * margin) / (n - 1);
            int x2 = margin + i * (width - 2 * margin) / (n - 1);

            int y1 = margin + (int) ((maxPrice - prices.get(i - 1)) / (maxPrice - minPrice) * (height - 2 * margin));
            int y2 = margin + (int) ((maxPrice - prices.get(i)) / (maxPrice - minPrice) * (height - 2 * margin));

            if (prices.get(i) >= prices.get(i - 1)) {
                g2d.setColor(Color.GREEN);
            } else {
                g2d.setColor(Color.RED);
            }
            g2d.drawLine(x1, y1, x2, y2);
        }

        // Draw month/year labels at bottom (depending on length of data)
        g2d.setColor(Color.YELLOW);
        g2d.setFont(new Font("Consolas", Font.BOLD, 12));

        LocalDate lastLabeled = null;
        for (int i = 0; i < n; i++) {
            int x = margin + i * (width - 2 * margin) / (n - 1);

            LocalDate date = Instant.ofEpochSecond(timestamps.get(i))
                    .atZone(ZoneId.systemDefault()).toLocalDate();

            boolean isNearStart = date.getDayOfMonth() <= 5;
            boolean isNearEnd = date.getDayOfMonth() >= date.lengthOfMonth() - 5;

            if ((lastLabeled == null || !sameMonthYear(date, lastLabeled)) && !isNearStart && !isNearEnd) {
                String monthLabel = date.format(DateTimeFormatter.ofPattern("MMM"));
                g2d.drawString(monthLabel, x - 10, height - margin / 3);
                lastLabeled = date;
            }
        }

        // Draw hover crosshair if mouse is on grid
        if (hoverX != null) {
            g2d.setColor(Color.GRAY);
            g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0));

            int usableWidth = width - 2 * margin;
            int usableHeight = height - 2 * margin;
            int nearestIdx = Math.min(n - 1, Math.max(0, (hoverX - margin) * (n - 1) / usableWidth));

            int x = margin + nearestIdx * usableWidth / (n - 1);
            int y = margin + (int) ((maxPrice - prices.get(nearestIdx)) / (maxPrice - minPrice) * usableHeight);

            // Vertical line
            g2d.drawLine(x, margin, x, height - margin);
            // Horizontal line
            g2d.drawLine(margin, y, width - margin, y);

            // Labels
            String dateLabel = Instant.ofEpochSecond(timestamps.get(nearestIdx))
                    .atZone(ZoneId.systemDefault()).toLocalDate()
                    .format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));

            String priceLabel = String.format("$%.2f", prices.get(nearestIdx));

            Font font = new Font("Consolas", Font.PLAIN, 12);
            g2d.setFont(font);
            FontMetrics metrics = g2d.getFontMetrics(font);

            int dateLabelWidth = metrics.stringWidth(dateLabel);
            int dateLabelHeight = metrics.getHeight();

            // Draw white background behind hover date
            int dateBoxX = Math.max(margin, x - dateLabelWidth / 2 - 5);
            int dateBoxY = height - margin / 2 - dateLabelHeight;

            g2d.setColor(new Color(0, 0, 0, 200));
            g2d.fillRect(dateBoxX, dateBoxY, dateLabelWidth + 10, dateLabelHeight);

            g2d.setColor(Color.WHITE);
            g2d.drawString(dateLabel, dateBoxX + 5, dateBoxY + metrics.getAscent());

            // Draw price label to the right of chart
            g2d.drawString(priceLabel, width - margin + 5, y);
        }
    }

    private boolean sameMonthYear(LocalDate d1, LocalDate d2) {
        return d1.getMonthValue() == d2.getMonthValue() && d1.getYear() == d2.getYear();
    }

//    private double roundToNiceNumber(double num) {
//        if (num <= 1) return 0.1;
//        else if (num <= 2) return 0.2;
//        else if (num <= 5) return 0.5;
//        else if (num <= 10) return 1;
//        else if (num <= 20) return 2;
//        else if (num <= 50) return 5;
//        else if (num <= 100) return 10;
//        else if (num <= 200) return 20;
//        else if (num <= 500) return 50;
//        else return 100;
//    }

    private double roundToNiceNumber(double num, int height) {
        double approxLabels = height / 25.0;
        double targetStep = num / approxLabels;

        if (targetStep <= 0.2) return 0.5;
        else if (targetStep <= 0.5) return 1;
        else if (targetStep <= 1) return 2;
        else if (targetStep <= 2) return 5;
        else if (targetStep <= 5) return 10;
        else if (targetStep <= 10) return 20;
        else if (targetStep <= 20) return 50;
        else if (targetStep <= 50) return 100;
        else return 200;
    }
}
