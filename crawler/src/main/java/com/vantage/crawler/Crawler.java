package com.vantage.crawler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Crawler extends Thread {

	private static Topic[] topicArray;
	private CloseableHttpClient httpClient;
	private HttpGet httpGet;
	private int id;

	public Crawler() {
		this.httpClient = HttpClients.custom().build();
	}

	public Crawler(CloseableHttpClient httpClient, HttpGet httpGet, int id) {
		this.httpClient = httpClient;
		this.httpGet = httpGet;
		this.id = id;
	}

	/**
	 * Compiles a list of the topic names and their corresponding urls
	 * @return ArrayList<Pair<String, String>>
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	private ArrayList<Pair<String, String>> getTopics() throws ClientProtocolException, IOException {
		ArrayList<Pair<String, String>> topicList = new ArrayList<Pair<String, String>>();

		Document topicPage = getWebpage("https://www.cochranelibrary.com/cdsr/reviews/topics");
		Elements topics = topicPage.getElementsByClass("browse-by-list-item");

		for (Element topic : topics) {
			String topicName = topic.getElementsByClass("browse-by-list-item-link").text();
			String topicUrl = topic.getElementsByTag("a").get(0).attr("href");

			Pair<String, String> pair = Pair.of(topicName, topicUrl);
			topicList.add(pair);
		}

		return topicList;
	}

	/**
	 * Sets up a connection pool allowing multiple threads to crawl individual topic pages
	 * @return Topic[]
	 * @throws Exception
	 */
	public Topic[] crawl() throws Exception {
		// Setup http config to allow circular redirects and to prevent invalid cookies
		RequestConfig requestConfig = RequestConfig.custom().setCircularRedirectsAllowed(true).setCookieSpec(CookieSpecs.STANDARD).build();
		PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();

		// Set the maximum number of connections in the pool
		connManager.setMaxTotal(100);

		CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(connManager).build();
		ArrayList<Crawler> threadList = new ArrayList<Crawler>();

		ArrayList<Pair<String, String>> topicList = getTopics();
		topicArray = new Topic[topicList.size()];

		for (int i = 0; i < topicList.size(); i++) {
			String topicUrl = topicList.get(i).getRight();

			// Prevent http client from caching response and set user agent to prevent http code 419 issues
			HttpGet httpGet = new HttpGet(topicUrl);
			httpGet.setHeader("Cache-Control", "no-cache");
			httpGet.setHeader("Pragma", "no-cache");
			httpGet.setHeader("X-Requested-With", "XMLHttpRequest");
			httpGet.setHeader("user-agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.90 Safari/537.36");
			httpGet.setConfig(requestConfig);
			
			// Create new crawler instances
			Crawler thread = new Crawler(httpClient, httpGet, i);
			threadList.add(thread);
			
			// Create new topic instance with the topic name and the topic url
			topicArray[i] = new Topic(topicList.get(i).getLeft(), topicUrl);
		}

		// Start each thread to visit an individual topic url
		for (int i = 0; i< threadList.size(); i++) {
			threadList.get(i).start();
		}

		// Wait until all threads are done crawling
		for (int i = 0; i< threadList.size(); i++) {
			threadList.get(i).join();
		}
		
		return topicArray;
	}

	/**
	 * Gets the web page given the url and returns a document 
	 * @param url String
	 * @return Document
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	private Document getWebpage(String url) throws ClientProtocolException, IOException {
		RequestConfig httpGetConfig = RequestConfig.custom().setCircularRedirectsAllowed(true).setCookieSpec(CookieSpecs.STANDARD).build();

		// Prevent http client from caching response and set user agent to prevent http code 419 issues
		HttpGet requestGet = new HttpGet(url);
		requestGet.setHeader("Cache-Control", "no-cache");
		requestGet.setHeader("Pragma", "no-cache");
		requestGet.setHeader("X-Requested-With", "XMLHttpRequest");
		requestGet.setHeader("user-agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.90 Safari/537.36");
		requestGet.setConfig(httpGetConfig);
		
		// Visit the actual page and parse the response
		HttpResponse response = httpClient.execute(requestGet);
		HttpEntity entity = response.getEntity();
		String responseString = EntityUtils.toString(entity, "UTF-8");

		return Jsoup.parse(responseString);
	}
	
	/**
	 * Visit each article page and grab the metadata for each article
	 */
	@Override
	public void run() {
		try {
			String topicUrl = httpGet.getURI().toString();
			String currentUrl = topicUrl;

			System.out.println(id + ". " + topicArray[id].getTopicName() + ": CRAWLING");

			while (currentUrl != null) {
				Document articlePage = getWebpage(currentUrl);
				Elements articles = articlePage.getElementsByClass("search-results-item-body");

				for (Element articleElement : articles) {
					String articleUrl = "https://www.cochranelibrary.com" + articleElement.getElementsByTag("a").get(0).attr("href");
					String articleName = articleElement.getElementsByClass("result-title").text();
					String articleAuthor = articleElement.getElementsByClass("search-result-authors").text();
					String articleDate = articleElement.getElementsByClass("search-result-metadata-item").get(2).text();

					Article article = new Article(articleUrl, articleName, articleAuthor, articleDate);
					topicArray[id].addArticle(article);
				}

				currentUrl = getNextPage(topicUrl, articlePage);
			}

			System.out.println(id + ". " + topicArray[id].getTopicName() + ": DONE");

		} catch(Exception e) {
			System.out.println(id +". " + e.getMessage());
		}
	}

	/**
	 * Checks if a topic page has a next link and then builds the next url to be visited
	 * @param topicUrl String	
	 * @param articlePage Element
	 * @return String
	 */
	private String getNextPage(String topicUrl, Element articlePage) {
		Elements nextElement = articlePage.getElementsByClass("pagination-next-link");

		if (nextElement != null && !nextElement.isEmpty()) {
			Elements anchorTag = nextElement.get(0).getElementsByTag("a");

			if (anchorTag != null && !anchorTag.isEmpty()) {
				String nextUrl = articlePage.getElementsByClass("pagination-next-link").get(0).getElementsByTag("a").get(0).attr("href");
				
				// Using regex to grab the cur query parameter from the url and the number if it exists
				Pattern regex = Pattern.compile("cur=(.*)");
				Matcher match = regex.matcher(nextUrl);
				
				// If a number is found then append it onto the topic url along with the number of results per page
				if (match.find()) {
					int num = Integer.valueOf(match.group().replace("cur=", ""));
					String newUrl = topicUrl + "&cur=" + num + "&resultPerPage=100";
					return newUrl;
				}
			}
		}
		
		// No next link found thus thread is on last page
		return null;
	}
}
