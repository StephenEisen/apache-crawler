package com.vantage.crawler;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class Article {

	private String articleUrl;
	private String articleName;
	private String articleAuthor;
	private String articleDate;
	
	public Article(String articleUrl, String articleName, String articleAuthor, String articleDate){
		this.articleUrl = articleUrl;
		this.articleName = articleName;
		this.articleAuthor = articleAuthor;
		this.articleDate = articleDate;
	}
	
	public String getArticleUrl() {
		return articleUrl;
	}
	public void setArticleUrl(String articleUrl) {
		this.articleUrl = articleUrl;
	}
	public String getArticleName() {
		return articleName;
	}
	public void setArticleName(String articleName) {
		this.articleName = articleName;
	}
	public String getArticleAuthor() {
		return articleAuthor;
	}
	public void setArticleAuthor(String articleAuthor) {
		this.articleAuthor = articleAuthor;
	}
	public String getArticleDate() {
		// Convert the scrapped date to the following format: yyyy-mm-dd
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH);
		LocalDate date = LocalDate.parse(articleDate, formatter);
		return date.toString();
	}
	public void setArticleDate(String articleDate) {
		this.articleDate = articleDate;
	}
}
