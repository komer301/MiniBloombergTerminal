package com.minibloomberg.ui;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class ComponentFactory {
    private static final JTextField searchField = new JTextField();
    private static final JButton searchButton = new JButton("Search");

    public static JPanel createTopPanel() {
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
        topPanel.setBackground(ColorPalette.EERIE_BLACK);
        topPanel.setPreferredSize(new Dimension(0, 80));

        styleSearchField();
        styleSearchButton();

        topPanel.add(searchField);
        topPanel.add(searchButton);

        return topPanel;
    }

    private static void styleSearchField() {
        searchField.setPreferredSize(new Dimension(300, 35));
        searchField.setFont(new Font("Consolas", Font.PLAIN, 16));
        searchField.setBackground(ColorPalette.JET);
        searchField.setForeground(ColorPalette.ANTI_FLASH_WHITE);
        searchField.setCaretColor(ColorPalette.ANTI_FLASH_WHITE);
        searchField.setOpaque(true);
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ColorPalette.ORANGE_PEEL, 2),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
    }

    private static void styleSearchButton() {
        searchButton.setPreferredSize(new Dimension(100, 35));
        searchButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        searchButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        searchButton.setForeground(ColorPalette.ANTI_FLASH_WHITE);
        searchButton.setOpaque(true);
        searchButton.setContentAreaFilled(false);
        searchButton.setBorder(BorderFactory.createLineBorder(ColorPalette.ORANGE_PEEL));
        searchButton.setBackground(ColorPalette.EERIE_BLACK);

        searchButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                searchButton.setContentAreaFilled(true);
                searchButton.setBackground(ColorPalette.ORANGE_PEEL);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                searchButton.setContentAreaFilled(false);
                searchButton.setBackground(ColorPalette.EERIE_BLACK);
            }
        });
    }

    public static JTextField getSearchField() {
        return searchField;
    }

    public static JButton getSearchButton() {
        return searchButton;
    }

    public static JPanel getEmptyPlaceholder() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(ColorPalette.NIGHT);

        JLabel label = new JLabel("Search for a stock to get started.");
        label.setForeground(ColorPalette.SILVER);
        label.setFont(new Font("Consolas", Font.ITALIC, 18));

        panel.add(label);
        return panel;
    }
}
