package com.minibloomberg.data;

public record NewsArticle(String title, String url, String source, String timePublished, String summary) {

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
