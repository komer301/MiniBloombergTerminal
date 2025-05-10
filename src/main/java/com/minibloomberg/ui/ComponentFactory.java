package com.minibloomberg.ui;

import javax.swing.*;
import java.awt.*;

public class ComponentFactory {
    private static final JTextField searchField = new JTextField();
    private static final JButton searchButton = new JButton("Search");

    public static JPanel createTopPanel() {
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
        topPanel.setBackground(new Color(0x252525));
        topPanel.setPreferredSize(new Dimension(0, 80));

        searchField.setPreferredSize(new Dimension(300, 35));
        searchField.setFont(new Font("Consolas", Font.PLAIN, 16));
        searchField.setBackground(new Color(0x1e1e1e));
        searchField.setForeground(Color.YELLOW);
        searchField.setCaretColor(Color.YELLOW);
        searchField.setBorder(BorderFactory.createLineBorder(Color.YELLOW));

        searchButton.setPreferredSize(new Dimension(50, 25));
        searchButton.setForeground(Color.YELLOW);
        searchButton.setBackground(new Color(0x1e1e1e));
        searchButton.setBorder(BorderFactory.createLineBorder(Color.YELLOW));
        searchButton.setFont(new Font("Consolas", Font.BOLD, 14));
        searchButton.setBorderPainted(false);
        searchButton.setFocusPainted(false);
        searchButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        return topPanel;
    }

    public static JTextField getSearchField() {
        return searchField;
    }

    public static JButton getSearchButton() {
        return searchButton;
    }

    public static JPanel getEmptyPlaceholder() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.BLACK);
        JLabel label = new JLabel("Search for a stock to get started.");
        label.setForeground(Color.GRAY);
        label.setFont(new Font("Consolas", Font.ITALIC, 18));
        panel.add(label);
        return panel;
    }
}
