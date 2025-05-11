package com.minibloomberg.data;

/**
 * Represents a single news article related to financial markets.
 *
 * @param title   The title or headline of the article.
 * @param url        A link to the full article.
 * @param source     The source of the news (e.g. CNBC, Reuters).
 * @param timePublished  The published date/time of the article.
 * @param summary    A short summary or excerpt of the article content.
 */
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
