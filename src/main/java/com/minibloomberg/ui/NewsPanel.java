package com.minibloomberg.ui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

import com.minibloomberg.data.NewsArticle;
import com.minibloomberg.logic.NewsFetcher;

public class NewsPanel extends JPanel {
    private final JPanel newsListPanel;
    private final JScrollPane scrollPane;
    private final Timer autoScrollTimer;
    private boolean isMouseOver = false;

    public NewsPanel() {
        setLayout(new BorderLayout());
        setBackground(ColorPalette.EERIE_BLACK);

        JLabel title = new JLabel("Market News");
        title.setForeground(ColorPalette.ANTI_FLASH_WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setBorder(new EmptyBorder(15, 15, 10, 0));
        title.setHorizontalAlignment(SwingConstants.CENTER);
        add(title, BorderLayout.NORTH);

        newsListPanel = new JPanel();
        newsListPanel.setLayout(new BoxLayout(newsListPanel, BoxLayout.Y_AXIS));
        newsListPanel.setBackground(ColorPalette.EERIE_BLACK);

        scrollPane = new JScrollPane(newsListPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setBackground(ColorPalette.JET);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);

        // Pause scrolling when mouse hovers over the panel
        newsListPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isMouseOver = true;
                autoScrollTimer.stop();
            }
            @Override
            public void mouseExited(MouseEvent e) {
                isMouseOver = false;
                autoScrollTimer.start();
            }
        });

        autoScrollTimer = new Timer(50, e -> autoScrollStep());

        // Refresh news every 10 minutes
        Timer refreshTimer = new Timer(600_000, e -> fetchNewsInBackground());
        refreshTimer.setRepeats(true);
        refreshTimer.start();

        fetchNewsInBackground();
    }

    public void setArticles(List<NewsArticle> articles) {
        newsListPanel.removeAll();

        for (NewsArticle article : articles) {
            newsListPanel.add(createArticlePanel(article));
            newsListPanel.add(Box.createVerticalStrut(16)); // Add space between articles
        }

        newsListPanel.revalidate();
        newsListPanel.repaint();

        // Reset scroll to top and start auto-scrolling if not paused
        SwingUtilities.invokeLater(() -> {
            scrollPane.getVerticalScrollBar().setValue(0);
            if (!autoScrollTimer.isRunning() && !isMouseOver) {
                autoScrollTimer.start();
            }
        });
    }

    private JPanel createArticlePanel(NewsArticle article) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(ColorPalette.JET);
        panel.setBorder(new EmptyBorder(12, 15, 24, 15));
        panel.setMaximumSize(new Dimension(9999, 120));

        // Headline (clickable)
        String safeTitle = NewsFetcher.truncateSummary(article.title(), 70);
        JLabel titleLabel = new JLabel("<html><div style='width:260px; font-weight:bold; word-break:break-word;'>" +
                "<u>" + safeTitle + "</u></div></html>");
        titleLabel.setForeground(ColorPalette.ORANGE_PEEL);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        titleLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        titleLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI(article.url()));
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(panel, "Could not open link.");
                }
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                isMouseOver = true;
                titleLabel.setForeground(ColorPalette.ORANGE_WEB);
                if (autoScrollTimer.isRunning()) autoScrollTimer.stop();
            }
            @Override
            public void mouseExited(MouseEvent e) {
                isMouseOver = false;
                titleLabel.setForeground(ColorPalette.ORANGE_PEEL);
                if (!autoScrollTimer.isRunning()) autoScrollTimer.start();
            }
        });

        // Meta info (source and publish date)
        String published = formatDate(article.timePublished());
        JLabel metaLabel = new JLabel(article.source() + " • " + published);
        metaLabel.setForeground(ColorPalette.SILVER);
        metaLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        // Article summary
        JLabel summaryLabel = new JLabel("<html><div style='width:260px; color:#cccccc; word-break:break-word;'>" +
                article.summary() + "</div></html>");
        summaryLabel.setForeground(ColorPalette.SILVER);
        summaryLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        summaryLabel.setBorder(new EmptyBorder(7, 0, 0, 0));

        panel.add(titleLabel);
        panel.add(metaLabel);
        panel.add(summaryLabel);
        panel.setMaximumSize(new Dimension(350, 150));

        return panel;
    }

    private String formatDate(String unixTime) {
        if (unixTime == null || unixTime.isEmpty()) return "";
        try {
            long ts = Long.parseLong(unixTime) * 1000L;
            java.util.Date date = new java.util.Date(ts);
            java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("MMM d, h:mm a");
            return formatter.format(date);
        } catch (Exception e) {
            return unixTime;
        }
    }

    private void fetchNewsInBackground() {
        new SwingWorker<List<NewsArticle>, Void>() {
            @Override
            protected List<NewsArticle> doInBackground() {
                return NewsFetcher.fetchLatestNews();
            }
            @Override
            protected void done() {
                try {
                    List<NewsArticle> articles = get();
                    setArticles(articles);
                } catch (Exception ex) {
                    setArticles(Collections.emptyList());
                    System.err.println("Error fetching news: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void autoScrollStep() {
        JScrollBar vBar = scrollPane.getVerticalScrollBar();
        int max = vBar.getMaximum() - vBar.getVisibleAmount();
        int curr = vBar.getValue();

        if (curr >= max) {
            vBar.setValue(0); // Restart scroll from top
        } else {
            vBar.setValue(curr + 1);
        }
    }
}
