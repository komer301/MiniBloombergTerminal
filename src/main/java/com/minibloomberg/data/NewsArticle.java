package com.minibloomberg.data;

public class NewsArticle {
    private String title;
    private String url;
    private String source;
    private String timePublished;
    private String summary;

    public NewsArticle(String title, String url, String source, String timePublished, String summary) {
        this.title = title;
        this.url = url;
        this.source = source;
        this.timePublished = timePublished;
        this.summary = summary;
    }

    // Getters
    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

    public String getSource() {
        return source;
    }

    public String getTimePublished() {
        return timePublished;
    }

    public String getSummary() {
        return summary;
    }

    @Override
    public String toString() {
        return "NewsArticle{" +
                "title='" + title + '\'' +
                ", url='" + url + '\'' +
                ", source='" + source + '\'' +
                ", timePublished='" + timePublished + '\'' +
                ", summary='" + summary + '\'' +
                '}';
    }
}
