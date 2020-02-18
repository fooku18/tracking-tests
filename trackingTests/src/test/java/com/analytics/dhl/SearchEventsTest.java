package com.analytics.dhl;

import static org.junit.Assert.*;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.client.ClientUtil;
import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.core.har.HarEntry;
import net.lightbody.bmp.core.har.HarLog;

public class SearchEventsTest {
	static BrowserMobProxyServer proxy;
    private WebDriver driver;
    private boolean headless;

	@BeforeClass
	public static void setUpClass() {
		proxy = new BrowserMobProxyServer();
		proxy.start();
	}
	
	@AfterClass
	public static void tearDownClass() {
		proxy.stop();
	}
	
    @Before
    public void setup() {
        Proxy seleniumProxy = ClientUtil.createSeleniumProxy(proxy);
        
        try {
            String hostIp = Inet4Address.getLocalHost().getHostAddress();
            seleniumProxy.setHttpProxy(hostIp + ":" + proxy.getPort());
            seleniumProxy.setSslProxy(hostIp + ":" + proxy.getPort());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        
        DesiredCapabilities seleniumCapabilities = new DesiredCapabilities();
        seleniumCapabilities.setCapability(CapabilityType.PROXY, seleniumProxy);
        headless = System.getProperty("com.analytics.dhl.head", null) != null ? false : true;
        if(System.getProperty("com.analytics.dhl.browser", "chrome").equals("chrome")) {
        	ChromeOptions chromeOptions = new ChromeOptions();
        	chromeOptions.merge(seleniumCapabilities);
        	chromeOptions.setHeadless(headless);
        	driver = new ChromeDriver(chromeOptions);
        }else{
        	FirefoxOptions firefoxOptions = new FirefoxOptions();
        	firefoxOptions.merge(seleniumCapabilities);
        	firefoxOptions.setHeadless(headless); 
        	driver = new FirefoxDriver(firefoxOptions);
        }
    }
    
    @After
    public void destroy(){
    	driver.quit();
    }
	
	@Test
	public void shouldContainSuccessfulInternalSearchData() {
		final String url = "https://www.logistics.dhl/global-en/home/search.html";
		final String searchInput = "dhl";
		Map<String,String> map = new HashMap<>();
		map.put("events", "event85,event83");
		map.put("c13", "dhl");
		map.put("v53", "dhl");
		map.put("v52", "*");
		
		driver.get(url);
		WebElement searchBar = null;
		WebElement submit = null;
		try {
			searchBar = driver.findElement(By.name("q"));
			submit = driver.findElement(By.cssSelector("button[type='submit']"));
		}catch(NoSuchElementException e) {
			fail(String.format("Search bar not found on page %s", url));
		}
		searchBar.sendKeys(searchInput);
		proxy.newHar("shouldContainSuccessfulInternalSearchEvents");
		submit.click();
		synchronized (driver) {
    		try {
				driver.wait(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
    	}
		Har har = proxy.getHar();
        
    	boolean isOkay = HarRequestMetricExtractor.checkForTrackedValue(har, map);
    	
        assertTrue(isOkay);
	}

	@Test
	public void shouldContainUnsuccessfulInternalSearchData() {
		final String url = "https://www.logistics.dhl/global-en/home/search.html";
		final String searchInput = "asdasdasd";
		Map<String,String> map = new HashMap<>();
		map.put("events", "event85,event84");
		map.put("c13", "asdasdasd");
		map.put("v53", "asdasdasd");
		
		driver.get(url);
		WebElement searchBar = null;
		WebElement submit = null;
		try {
			searchBar = driver.findElement(By.name("q"));
			submit = driver.findElement(By.cssSelector("button[type='submit']"));
		}catch(NoSuchElementException e) {
			fail(String.format("Search bar not found on page %s", url));
		}
		searchBar.sendKeys(searchInput);
		proxy.newHar("shouldContainUnsuccessfulInternalSearchData");
		submit.click();
		synchronized (driver) {
    		try {
				driver.wait(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
    	}
		Har har = proxy.getHar();
        
    	boolean isOkay = HarRequestMetricExtractor.checkForTrackedValue(har, map);
    	
        assertTrue(isOkay);
	}
}
