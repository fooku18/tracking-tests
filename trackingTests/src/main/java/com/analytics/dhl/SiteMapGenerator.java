package com.analytics.dhl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

public class SiteMapGenerator{
	private final static Logger logger = Logger.getLogger(SiteMapGenerator.class.getName());
	private final static String USERAGENT = "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.157 Safari/537.36";
	private final static String SITEMAPURL = "https://www.logistics.dhl/sitemap-index.xml";
	private SiteMapUrls siteMapUrls;
	private SiteMapLocale siteMapLocale;
	
	public static void main(String[] args) throws IOException {
		SiteMapGenerator generator = new SiteMapGenerator();
		List<String> list = generator.randomize(10);
		for(String item : list) {
			System.out.println(item);
		}
	}
	
	public SiteMapGenerator() throws IOException{
		siteMapLocale = new SiteMapLocale();
		siteMapUrls = new SiteMapUrls(siteMapLocale.getSiteMapLocalUrl());
	}
	
	public List<String> randomize() {
		return this.siteMapUrls.getRandomUrls(0);
	}
	
	public List<String> randomize(int maxUrls) {
		return this.siteMapUrls.getRandomUrls(maxUrls);
	}
	
	private class SiteMapUrls{
		private List<String> siteMapPageUrls;
		
		private SiteMapUrls(String siteMapUrl) throws IOException {
			this.getUrls(siteMapUrl);
		}
		
		private void getUrls(String siteMapUrl) throws IOException {
			Document doc = Jsoup.connect(siteMapUrl)
							.userAgent(USERAGENT)
							.timeout(240 * 1000)
							.parser(Parser.xmlParser())
							.get();
			
			this.siteMapPageUrls = doc.select("loc").stream()
									.map(loc -> loc.text())
									.collect(Collectors.toList());
		}
		
		public void newSiteMap(String siteMapUrl) throws IOException {
			this.getUrls(siteMapUrl);
		}
		
		public List<String> getRandomUrls(int maxUrls) {
			List<String> shuffledList = new ArrayList<>();
			int size = siteMapPageUrls.size();
			int min = Math.min(size, maxUrls > 0 ? maxUrls : size);
			logger.log(Level.INFO, String.format("Generating random URLs with %d Items", min));
			
			List<String> indicesList = new ArrayList<>();
			for(String item : this.siteMapPageUrls) {
				indicesList.add(item);
			}
			
			min--;
			do {
				size = indicesList.size();
				double rand = Math.random();
				int randIndex = Double.valueOf(rand * size).intValue();
				shuffledList.add(indicesList.get(randIndex));
				indicesList.remove(randIndex);
			} while (--min > 0);
			return shuffledList;
		}
	}

	private class SiteMapLocale{
		private String siteMapLocalUrl;
		private Elements sitemapElements;
		private int retry = 0;
		
		private SiteMapLocale() throws IOException{
			if(siteMapLocalUrl == null) {
				this.getSitemaps();
				this.shuffle();
			}
		}
		
		public String getSiteMapLocalUrl() {
			return this.siteMapLocalUrl;
		}
		
		public String shuffle() throws IOException {
			double rand = Math.random();
			int index = Double.valueOf(rand * this.sitemapElements.size()).intValue();
			this.siteMapLocalUrl = sitemapElements.get(index).text();
			logger.log(Level.INFO, String.format("Using %s Sitemap for generating URLs", this.siteMapLocalUrl));
			try {
				Document check = Jsoup.connect(this.siteMapLocalUrl)
									.userAgent(USERAGENT)
									.timeout(240 * 1000)
									.parser(Parser.xmlParser())
									.get();
				this.retry = 0;
			} catch(HttpStatusException e) {
				if(++this.retry < 5) {
					logger.log(Level.INFO, String.format("Sitemap %s returned 404. Retrying with new URL --- Retry #%d", this.siteMapLocalUrl, this.retry));
					this.shuffle();
				}
			}
			return this.siteMapLocalUrl;
		}

		private void getSitemaps() throws IOException{
			Document sitemaps = null;
			sitemaps = Jsoup.connect(SITEMAPURL)
					.userAgent(USERAGENT)
					.parser(Parser.xmlParser())
					.get();
			this.sitemapElements = sitemaps.select("sitemap");
		}
	}
}