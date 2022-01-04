package com.vantage.crawler;

import java.util.ArrayList;

public class Topic {
	private String topicName;
	private String topicUrl;
	private ArrayList<Article> articleList = new ArrayList<Article>();
	
	public Topic(String topicName, String topicUrl) {
		this.topicName = topicName;
		this.topicUrl = topicUrl;
	}

	public String getTopicName() {
		return topicName;
	}

	public void setTopicName(String topicName) {
		this.topicName = topicName;
	}

	public String getTopicUrl() {
		return topicUrl;
	}

	public void setTopicUrl(String topicUrl) {
		this.topicUrl = topicUrl;
	}

	public ArrayList<Article> getArticleList() {
		return articleList;
	}

	public void setArticleList(ArrayList<Article> articleList) {
		this.articleList = articleList;
	}
	
	public void addArticle(Article article) {
		this.articleList.add(article);
	}
}
