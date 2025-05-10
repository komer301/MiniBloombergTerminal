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
        if (data == null || data.timestamps().isEmpty()) return;

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        int marginX = 70;
        int topMargin = 35;
        int bottomMargin = 70;

        List<Double> prices = data.closePrices();

        // Font setup for labels
        Font labelFont = new Font("Consolas", Font.BOLD, 12);
        g2.setFont(labelFont);
        FontMetrics fm = g2.getFontMetrics();

        int usableHeight = height - topMargin - bottomMargin;

        double minPrice = prices.stream().min(Double::compareTo).orElse(0.0);
        double maxPrice = prices.stream().max(Double::compareTo).orElse(1.0);
        double priceRange = maxPrice - minPrice;

        if (priceRange < 0.01) {
            double center = (minPrice + maxPrice) / 2.0;
            minPrice = center - 0.25;
            maxPrice = center + 0.25;
        } else if (priceRange < 1.0) {
            double padding = priceRange * 2.5;
            minPrice = Math.max(0, minPrice - padding * 0.4);
            maxPrice += padding * 0.6;
        } else {
            double padding = priceRange * 0.1;
            minPrice = Math.max(0, minPrice - padding * 0.4);
            maxPrice += padding * 0.6;
        }

        double adjustedRange = maxPrice - minPrice;

        // Ensure visual separation even for low-variation charts
        int pixelRange = (int)((maxPrice - minPrice) / adjustedRange * usableHeight);
        int minVisualPixels = 100;
        if (pixelRange < minVisualPixels) {
            double extra = (minVisualPixels - pixelRange) / (double) usableHeight * adjustedRange;
            minPrice -= extra / 2;
            maxPrice += extra / 2;
            adjustedRange = maxPrice - minPrice;
        }

        // Draw Y-axis gridlines and labels
        double step = roundToNiceNumber(adjustedRange, usableHeight);
        g2.setColor(Color.LIGHT_GRAY);
        for (double p = Math.ceil(minPrice / step) * step; p <= maxPrice; p += step) {
            int y = topMargin + (int) ((maxPrice - p) / adjustedRange * usableHeight);
            int labelX = marginX / 4;  // Centered within left margin
            g2.drawString(String.format("%.2f", p), labelX, y + 5);

            g2.setColor(new Color(200, 200, 200, 50));
            g2.drawLine(marginX, y, width - marginX, y);
            g2.setColor(Color.LIGHT_GRAY);
        }

        // Draw price lines
        int n = prices.size();
        g2.setStroke(new BasicStroke(2));
        for (int i = 1; i < n; i++) {
            int x1 = marginX + (i - 1) * (width - 2 * marginX) / (n - 1);
            int x2 = marginX + i * (width - 2 * marginX) / (n - 1);
            int y1 = topMargin + (int) ((maxPrice - prices.get(i - 1)) / adjustedRange * usableHeight);
            int y2 = topMargin + (int) ((maxPrice - prices.get(i)) / adjustedRange * usableHeight);

            g2.setColor(prices.get(i) >= prices.get(i - 1) ? Color.GREEN : Color.RED);
            g2.drawLine(x1, y1, x2, y2);
        }

        // Draw X-axis labels (35 px up from bottom)
        int yLabel = height - 45 + fm.getAscent();
        g2.setFont(labelFont);
        g2.setColor(Color.YELLOW);

        int totalDays = (int) java.time.temporal.ChronoUnit.DAYS.between(
                Instant.ofEpochSecond(data.timestamps().get(0)).atZone(ZoneId.systemDefault()).toLocalDate(),
                Instant.ofEpochSecond(data.timestamps().get(n - 1)).atZone(ZoneId.systemDefault()).toLocalDate());

        LocalDate lastLabeled = null;
        for (int i = 0; i < n; i++) {
            int x = marginX + i * (width - 2 * marginX) / (n - 1);
            LocalDate date = Instant.ofEpochSecond(data.timestamps().get(i)).atZone(ZoneId.systemDefault()).toLocalDate();

            if (totalDays <= 10) {
                if (!date.equals(lastLabeled)) {
                    String label = date.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd"));
                    int strW = fm.stringWidth(label);
                    g2.drawString(label, x - strW / 2, yLabel);
                    lastLabeled = date;
                }
            } else if (totalDays <= 370) {
                if (lastLabeled == null || !sameMonthYear(date, lastLabeled)) {
                    boolean isNearStart = date.getDayOfMonth() <= 5;
                    boolean isNearEnd = date.getDayOfMonth() >= date.lengthOfMonth() - 5;
                    if (!isNearStart && !isNearEnd) {
                        String label = date.format(java.time.format.DateTimeFormatter.ofPattern("MMM"));
                        int strW = fm.stringWidth(label);
                        g2.drawString(label, x - strW / 2, yLabel);
                        lastLabeled = date;
                    }
                }
            } else {
                int yearGap = (totalDays > 3650) ? 5 : (totalDays > 1825) ? 2 : 1;
                if (lastLabeled == null || date.getYear() >= lastLabeled.getYear() + yearGap) {
                    String label = String.valueOf(date.getYear());
                    int strW = fm.stringWidth(label);
                    g2.drawString(label, x - strW / 2, yLabel);
                    lastLabeled = date;
                }
            }
        }

        // Hover crosshair
        if (hoverX != null) {
            int usableWidth = width - 2 * marginX;
            int nearestIdx = Math.min(n - 1, Math.max(0, (hoverX - marginX) * (n - 1) / usableWidth));
            int x = marginX + nearestIdx * usableWidth / (n - 1);
            int y = topMargin + (int) ((maxPrice - prices.get(nearestIdx)) / adjustedRange * usableHeight);

            g2.setColor(Color.GRAY);
            g2.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0));
            g2.drawLine(x, topMargin, x, topMargin + usableHeight);
            g2.drawLine(marginX, y, width - marginX, y);

            String dateLabel = Instant.ofEpochSecond(data.timestamps().get(nearestIdx))
                    .atZone(ZoneId.systemDefault()).toLocalDate()
                    .format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy"));
            String priceLabel = String.format("$%.2f", prices.get(nearestIdx));

            Font hoverFont = new Font("Consolas", Font.PLAIN, 12);
            g2.setFont(hoverFont);
            FontMetrics hoverMetrics = g2.getFontMetrics(hoverFont);

            int dateLabelWidth = hoverMetrics.stringWidth(dateLabel);
            int dateLabelHeight = hoverMetrics.getHeight();
            int dateBoxX = Math.max(marginX, x - dateLabelWidth / 2 - 5);
            int dateBoxY = height - 35 + 5;

            g2.setColor(new Color(0, 0, 0, 200));
            g2.fillRect(dateBoxX, dateBoxY, dateLabelWidth + 10, dateLabelHeight);
            g2.setColor(Color.WHITE);
            g2.drawString(dateLabel, dateBoxX + 5, dateBoxY + hoverMetrics.getAscent());
            g2.drawString(priceLabel, width - marginX + 5, y);
        }
    }

    private boolean sameMonthYear(LocalDate d1, LocalDate d2) {
        return d1.getMonthValue() == d2.getMonthValue() && d1.getYear() == d2.getYear();
    }

    private double roundToNiceNumber(double num, int height) {
        double approxLabels = height / 30.0; // number of desired Y labels
        double rawStep = num / approxLabels;

        // Choose from a clean list of reasonable step sizes
        double[] steps = {0.01, 0.02, 0.05, 0.1, 0.2, 0.5, 1, 2, 5, 10, 20, 50, 100};

        for (double step : steps) {
            if (rawStep <= step) return step;
        }

        return 200; // fallback for very large ranges
    }
}
