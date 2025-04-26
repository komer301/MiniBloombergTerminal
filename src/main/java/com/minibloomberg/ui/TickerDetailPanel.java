package com.minibloomberg.ui;

import com.minibloomberg.logic.Stock;
import com.minibloomberg.logic.StockDataFetcher;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class TickerDetailPanel extends JPanel {

    public TickerDetailPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(new Color(0xCFCCCC));
        setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(0xDDDDDD), 1),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        JPanel infoPanel = new JPanel();
        infoPanel.setBackground(new Color(0xFFFFFF));
        infoPanel.setLayout(new GridLayout(4, 2, 10, 10));
        infoPanel.setBorder(BorderFactory.createTitledBorder("Stock Info"));
        Stock snapshot = com.minibloomberg.logic.StockDataFetcher.fetchStockSnapshot("TSLA");
        assert snapshot != null;
        infoPanel.add(new JLabel("Symbol: " + snapshot.symbol));
        infoPanel.add(new JLabel("Company: " + snapshot.companyName));
        infoPanel.add(new JLabel("Change: " + snapshot.change));
        infoPanel.add(new JLabel("Percent Change: " + snapshot.percentChange));
        infoPanel.add(new JLabel("Previous Close: " + snapshot.previousClose));
        infoPanel.add(new JLabel("Current Price: " + snapshot.currentPrice));
        infoPanel.add(new JLabel("Day Low: " + snapshot.dayLow));
        infoPanel.add(new JLabel("Day High: " + snapshot.dayHigh));
        infoPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        add(infoPanel);

        add(Box.createVerticalStrut(15));

        JPanel chartContainer = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0)); //
        chartContainer.setOpaque(false); //

        JPanel chartPanel = new JPanel();
        chartPanel.setBackground(Color.WHITE);

//        chartPanel.setBorder(BorderFactory.createTitledBorder("Chart"));
        chartPanel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(0xCCCCCC), 2 , true),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        chartContainer.add(chartPanel);

        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int parentWidth = getWidth();
                int newWidth = (int) (0.8 * parentWidth);
                chartPanel.setPreferredSize(new Dimension(newWidth, 300));
                chartPanel.revalidate();
            }
        });
        add(chartContainer);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JButton addToWatchlist = new JButton("Add to Watchlist");
        addToWatchlist.setPreferredSize(new Dimension(200, 40));
        buttonPanel.add(addToWatchlist);
        buttonPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 100, 0));
        add(buttonPanel);
    }
}
