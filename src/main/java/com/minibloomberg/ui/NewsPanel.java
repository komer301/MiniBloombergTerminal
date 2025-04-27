package com.minibloomberg.ui;

import java.awt.BorderLayout;
import java.awt.Color;
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

/**
 * NewsPanel displays a vertically auto-scrolling list of the latest market news articles.
 * Scrolling pauses when the user hovers over the article panel or a news headline.
 */
public class NewsPanel extends JPanel {
    private final JPanel newsListPanel;
    private final JScrollPane scrollPane;

    // Auto-scroll state
    private final Timer autoScrollTimer;
    private final int scrollSpeedMs = 50;  // Delay (ms) between scroll steps (smaller = faster)
    private final int scrollStepPx = 1;    // Pixels to scroll per step (smaller = smoother)
    private boolean isMouseOver = false;   // Tracks if mouse is hovering

    private final Timer refreshTimer;
    private final int refreshIntervalMs = 600_000; // refresh news every 10 minutes


    public NewsPanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(0x252525));

        JLabel title = new JLabel("Market News");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setBorder(new EmptyBorder(15, 15, 10, 0));
        title.setHorizontalAlignment(SwingConstants.CENTER);
        add(title, BorderLayout.NORTH);

        newsListPanel = new JPanel();
        newsListPanel.setLayout(new BoxLayout(newsListPanel, BoxLayout.Y_AXIS));
        newsListPanel.setBackground(new Color(0x252525));

        scrollPane = new JScrollPane(newsListPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setBackground(new Color(0x252525));
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER); // Hide vertical bar
        add(scrollPane, BorderLayout.CENTER);

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

        autoScrollTimer = new Timer(scrollSpeedMs, e -> autoScrollStep());
        refreshTimer = new Timer(refreshIntervalMs, e -> fetchNewsInBackground());
        refreshTimer.setRepeats(true);
        refreshTimer.start();

        fetchNewsInBackground();
    }

    /**
     * Renders the list of articles in the news panel and (re)starts scrolling.
     */
    public void setArticles(List<NewsArticle> articles) {
        newsListPanel.removeAll();

        for (NewsArticle article : articles) {
            newsListPanel.add(createArticlePanel(article));
            newsListPanel.add(Box.createVerticalStrut(16)); // Space between articles
        }

        newsListPanel.revalidate();
        newsListPanel.repaint();

        // Reset scroll to top & (re)start auto-scroll
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
        panel.setBackground(new Color(0x2d2d2d));
        panel.setBorder(new EmptyBorder(12, 15, 12, 15));
        panel.setMaximumSize(new Dimension(9999, 120));

        // Headline (title) label - clickable, underlined, pauses scroll on hover
        String safeTitle = NewsFetcher.truncateSummary(article.getTitle(), 70);
        JLabel titleLabel = new JLabel("<html><div style='width:260px; color:#62b6f7; font-weight:bold; word-break:break-word;'>" +
                "<u>" + safeTitle + "</u></div></html>");
        titleLabel.setForeground(new Color(0x62b6f7));
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        titleLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        titleLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI(article.getUrl()));
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(panel, "Could not open link.");
                }
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                isMouseOver = true;
                if (autoScrollTimer.isRunning()) autoScrollTimer.stop();
            }
            @Override
            public void mouseExited(MouseEvent e) {
                isMouseOver = false;
                if (!autoScrollTimer.isRunning()) autoScrollTimer.start();
            }
        });

        // Meta info (source + date)
        String published = formatDate(article.getTimePublished());
        JLabel metaLabel = new JLabel(article.getSource() + " • " + published);
        metaLabel.setForeground(new Color(0xbbbbbb));
        metaLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        // Summary label (text wraps)
        JLabel summaryLabel = new JLabel("<html><div style='width:260px; color:#cccccc; word-break:break-word;'>" +
                article.getSummary() + "</div></html>");
        summaryLabel.setForeground(Color.LIGHT_GRAY);
        summaryLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        summaryLabel.setBorder(new EmptyBorder(7, 0, 0, 0));

        panel.add(titleLabel);
        panel.add(metaLabel);
        panel.add(summaryLabel);
        panel.setMaximumSize(new Dimension(350, 150));
        return panel;
    }

    /**
     * Converts UNIX timestamp (seconds) to readable format.
     */
    private String formatDate(String unixTime) {
        if (unixTime == null || unixTime.isEmpty()) return "";
        try {
            long ts = Long.parseLong(unixTime) * 1000L;
            java.util.Date date = new java.util.Date(ts);
            java.text.SimpleDateFormat display = new java.text.SimpleDateFormat("MMM d, h:mm a");
            return display.format(date);
        } catch (Exception e) {
            return unixTime;
        }
    }
    

    /**
     * Asynchronously fetches latest news and updates panel.
     */
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

    /**
     * Scrolls the news list panel downward, loops back to top at end.
     */
    private void autoScrollStep() {
        JScrollBar vbar = scrollPane.getVerticalScrollBar();
        int max = vbar.getMaximum() - vbar.getVisibleAmount();
        int curr = vbar.getValue();
        if (curr >= max) {
            vbar.setValue(0); // Loop to top
        } else {
            vbar.setValue(curr + scrollStepPx);
        }
    }
}
