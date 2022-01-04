package com.vantage.crawler;

public class App 
{
    public static void main( String[] args ) throws Exception
    {
    	Crawler crawler = new Crawler();
    	Topic[] topicData = crawler.crawl();
    	
    	Writer writer = new Writer();
    	writer.writeFile(topicData);
    }
}
