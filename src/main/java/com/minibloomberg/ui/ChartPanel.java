package com.minibloomberg.ui;

import com.minibloomberg.data.HistoricalData;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

public class ChartPanel extends JPanel {
    private HistoricalData data;
    private Integer hoverX = null;

    public ChartPanel() {
        setBackground(ColorPalette.NIGHT);

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
        if (data == null || data.timestamps().isEmpty()) return;

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        int marginX = 70, topMargin = 35, bottomMargin = 70;
        int usableHeight = height - topMargin - bottomMargin;

        List<Double> prices = data.closePrices();

        Font labelFont = new Font("Consolas", Font.BOLD, 12);
        g2.setFont(labelFont);
        FontMetrics fm = g2.getFontMetrics();

        double minPrice = prices.stream().min(Double::compareTo).orElse(0.0);
        double maxPrice = prices.stream().max(Double::compareTo).orElse(1.0);
        double priceRange = maxPrice - minPrice;

        if (priceRange < 0.01) {
            double center = (minPrice + maxPrice) / 2.0;
            minPrice = center - 0.25;
            maxPrice = center + 0.25;
        } else if (priceRange < 1.0) {
            double pad = priceRange * 2.5;
            minPrice = Math.max(0, minPrice - pad * 0.4);
            maxPrice += pad * 0.6;
        } else {
            double pad = priceRange * 0.1;
            minPrice = Math.max(0, minPrice - pad * 0.4);
            maxPrice += pad * 0.6;
        }

        double adjustedRange = maxPrice - minPrice;

        // Enforce visual space even in flat charts
        int pixelRange = (int) ((maxPrice - minPrice) / adjustedRange * usableHeight);
        if (pixelRange < 100) {
            double extra = (100 - pixelRange) / (double) usableHeight * adjustedRange;
            minPrice -= extra / 2;
            maxPrice += extra / 2;
            adjustedRange = maxPrice - minPrice;
        }

        drawYAxis(g2, width, topMargin, marginX, usableHeight, minPrice, maxPrice, adjustedRange);
        drawPriceLines(g2, prices, width, topMargin, marginX, usableHeight, maxPrice, adjustedRange);
        drawXAxisLabels(g2, fm, width, height, marginX, data.timestamps());
        if (hoverX != null)
            drawHoverCrosshair(g2, fm, width, height, marginX, topMargin, usableHeight, prices, maxPrice, adjustedRange);
    }

    private void drawYAxis(Graphics2D g2, int width, int topMargin, int marginX, int usableHeight,
                           double minPrice, double maxPrice, double adjustedRange) {
        double step = roundToNiceNumber(adjustedRange, usableHeight);
        g2.setColor(ColorPalette.SILVER);
        for (double p = Math.ceil(minPrice / step) * step; p <= maxPrice; p += step) {
            int y = topMargin + (int) ((maxPrice - p) / adjustedRange * usableHeight);
            g2.drawString(String.format("%.2f", p), marginX / 4, y + 5);
            g2.setColor(new Color(200, 200, 200, 50));
            g2.drawLine(marginX, y, width - marginX, y);
            g2.setColor(ColorPalette.SILVER);
        }
    }

    private void drawPriceLines(Graphics2D g2, List<Double> prices, int width, int topMargin, int marginX,
                                int usableHeight, double maxPrice, double adjustedRange) {
        int n = prices.size();
        g2.setStroke(new BasicStroke(2));
        for (int i = 1; i < n; i++) {
            int x1 = marginX + (i - 1) * (width - 2 * marginX) / (n - 1);
            int x2 = marginX + i * (width - 2 * marginX) / (n - 1);
            int y1 = topMargin + (int) ((maxPrice - prices.get(i - 1)) / adjustedRange * usableHeight);
            int y2 = topMargin + (int) ((maxPrice - prices.get(i)) / adjustedRange * usableHeight);
            g2.setColor(prices.get(i) >= prices.get(i - 1) ? ColorPalette.GREEN : ColorPalette.RED);
            g2.drawLine(x1, y1, x2, y2);
        }
    }

    private void drawXAxisLabels(Graphics2D g2, FontMetrics fm, int width, int height, int marginX, List<Long> timestamps) {
        g2.setFont(new Font("Consolas", Font.BOLD, 12));
        g2.setColor(ColorPalette.ANTI_FLASH_WHITE);
        int yLabel = height - 45 + fm.getAscent();
        int n = timestamps.size();

        int totalDays = (int) java.time.temporal.ChronoUnit.DAYS.between(
                Instant.ofEpochSecond(timestamps.get(0)).atZone(ZoneId.systemDefault()).toLocalDate(),
                Instant.ofEpochSecond(timestamps.get(n - 1)).atZone(ZoneId.systemDefault()).toLocalDate());

        LocalDate lastLabeled = null;
        for (int i = 0; i < n; i++) {
            int x = marginX + i * (width - 2 * marginX) / (n - 1);
            LocalDate date = Instant.ofEpochSecond(timestamps.get(i)).atZone(ZoneId.systemDefault()).toLocalDate();
            String label = null;

            if (totalDays <= 10 && !date.equals(lastLabeled)) {
                label = date.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd"));
            } else if (totalDays <= 370 && (lastLabeled == null || !sameMonthYear(date, lastLabeled))) {
                if (date.getDayOfMonth() > 5 && date.getDayOfMonth() < date.lengthOfMonth() - 5) {
                    label = date.format(java.time.format.DateTimeFormatter.ofPattern("MMM"));
                }
            } else if (totalDays > 370) {
                int yearGap = (totalDays > 3650) ? 5 : (totalDays > 1825) ? 2 : 1;
                if (lastLabeled == null || date.getYear() >= lastLabeled.getYear() + yearGap) {
                    label = String.valueOf(date.getYear());
                }
            }

            if (label != null) {
                int strW = fm.stringWidth(label);
                g2.drawString(label, x - strW / 2, yLabel);
                lastLabeled = date;
            }
        }
    }

    private void drawHoverCrosshair(Graphics2D g2, FontMetrics fm, int width, int height, int marginX, int topMargin,
                                    int usableHeight, List<Double> prices, double maxPrice, double adjustedRange) {
        int n = prices.size();
        int usableWidth = width - 2 * marginX;
        int idx = Math.min(n - 1, Math.max(0, (hoverX - marginX) * (n - 1) / usableWidth));
        int x = marginX + idx * usableWidth / (n - 1);
        int y = topMargin + (int) ((maxPrice - prices.get(idx)) / adjustedRange * usableHeight);

        g2.setColor(Color.GRAY);
        g2.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0));
        g2.drawLine(x, topMargin, x, topMargin + usableHeight);
        g2.drawLine(marginX, y, width - marginX, y);

        String dateLabel = Instant.ofEpochSecond(data.timestamps().get(idx))
                .atZone(ZoneId.systemDefault()).toLocalDate()
                .format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy"));
        String priceLabel = String.format("$%.2f", prices.get(idx));

        Font hoverFont = new Font("Consolas", Font.PLAIN, 12);
        g2.setFont(hoverFont);
        FontMetrics hoverMetrics = g2.getFontMetrics();

        int labelWidth = hoverMetrics.stringWidth(dateLabel);
        int labelHeight = hoverMetrics.getHeight();
        int dateBoxX = Math.max(marginX, x - labelWidth / 2 - 5);
        int dateBoxY = height - 35 + 5;

        g2.setColor(new Color(0, 0, 0, 200));
        g2.fillRect(dateBoxX, dateBoxY, labelWidth + 10, labelHeight);
        g2.setColor(Color.WHITE);
        g2.drawString(dateLabel, dateBoxX + 5, dateBoxY + hoverMetrics.getAscent());
        g2.drawString(priceLabel, width - marginX + 5, y);
    }

    private boolean sameMonthYear(LocalDate d1, LocalDate d2) {
        return d1.getMonthValue() == d2.getMonthValue() && d1.getYear() == d2.getYear();
    }

    private double roundToNiceNumber(double num, int height) {
        double approxLabels = height / 30.0;
        double rawStep = num / approxLabels;
        double[] steps = {0.01, 0.02, 0.05, 0.1, 0.2, 0.5, 1, 2, 5, 10, 20, 50, 100};
        for (double step : steps) {
            if (rawStep <= step) return step;
        }
        return 200;
    }
}
