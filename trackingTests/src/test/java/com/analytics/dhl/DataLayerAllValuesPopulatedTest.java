/**
 * 
 */
package com.analytics.dhl;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

/**
 * @author dhl analytics
 *
 */
@RunWith(Parameterized.class)
public class DataLayerAllValuesPopulatedTest {
	private static final Logger logger = Logger.getLogger(DataLayerAllValuesPopulatedTest.class.getName());
	private static MongoClient mc;
	private static MongoCollection<org.bson.Document> mcoll;
	private final String USERAGENT = "JJ-Test";
	private String pageUrl;
	
	@BeforeClass
	public static void setUp() {
		try {
			mc = MongoClients.create();
			MongoDatabase db = mc.getDatabase("trackingTests");
			mcoll = db.getCollection("pages");
		}catch(Exception e) {}
	}
	
	@AfterClass
	public static void tearDown() {
		try {
			mc.close();
		}catch(Exception e) {}
	}
	
	public DataLayerAllValuesPopulatedTest(String pageUrl) {
		this.pageUrl = pageUrl;
	}
	
	@Parameters
	public static List<String[]> initValues() throws IOException{
		SiteMapGenerator siteMapGenerator = new SiteMapGenerator();
		List<String> pageUrls = siteMapGenerator.randomize();
		List<String[]> list = pageUrls.stream()
								.map(element -> new String[] {element})
								.collect(Collectors.toList());
		return list;
	}
	
	@Test
	public void shouldContainAllRequiredDataElementValues() {
		try {
			Document doc = Jsoup.connect(this.pageUrl)
							.userAgent(USERAGENT)
							.get();
			
			if(!doc.baseUri().contains("logistics.dhl")) {
				fail(String.format("%s redirects to non logistics page %s", this.pageUrl, doc.baseUri()));
			}
			Elements scriptTags = doc.select("script");
			for(Element scriptTag : scriptTags) {
				if(scriptTag.data().contains("dataLayer")) {
					Pattern r = Pattern.compile("(?<=(dataLayer = ))(.*})(?=;)");
					Matcher m = r.matcher(scriptTag.data());
					if(m.find()) {
						String dataLayerString = m.group(2);
						List<String> errors = DataLayerValidator.validate(dataLayerString, new String[] {
								"page.pageInfo.pageName",
								"page.pageInfo.language",
								"page.pageInfo.publisher",
								"page.category.primaryCategory",
								"page.category.subCategory1"
						});
						if(errors != null) {
							if(mcoll != null) {
								try {
									org.bson.Document document = new org.bson.Document();
									document.append("_id", this.pageUrl);
									document.append("errors", errors);
									org.bson.Document replacedDocument = mcoll.findOneAndReplace(com.mongodb.client.model.Filters.eq("_id", this.pageUrl), document);
									if(replacedDocument == null) {
										mcoll.insertOne(document);
									}
								}catch(MongoException e) {
									logger.log(Level.INFO, "MongoDB error");
								}
							}
							fail(String.format("Test failed for %s reason: %s", this.pageUrl, errors.stream().collect(Collectors.joining(", "))));
						}
						return;
					}
				}
			}
			fail(String.format("dataLayer not found in %s", this.pageUrl));
		} catch(IOException e) {
			e.getStackTrace();
		}
	}

}
