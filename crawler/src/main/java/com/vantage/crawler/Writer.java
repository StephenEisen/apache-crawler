package com.vantage.crawler;

import java.io.FileWriter;
import java.io.IOException;

public class Writer {

	/**
	 * Writes the topics list to a text file
	 * @param topics Topic[]
	 */
	public void writeFile(Topic[] topics) {
		try {
			FileWriter myWriter = new FileWriter("cochrane_reviews.txt");
			
			for (int i = 0; i < topics.length; i++) {
				String topicName = topics[i].getTopicName();

				for (int j = 0; j < topics[i].getArticleList().size(); j++) {
					Article article = topics[i].getArticleList().get(j);
					String articleUrl = article.getArticleUrl();
					String articleName = article.getArticleName();
					String articleAuthor = article.getArticleAuthor();
					String articleDate = article.getArticleDate();

					myWriter.write(articleUrl + "|" + topicName + "|" + articleName + "|" + articleAuthor + "|" + articleDate + "\r\n");
				}
			}

			myWriter.close();
			System.out.println("Successfully wrote to file.");
		} catch (IOException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}
	}
}
