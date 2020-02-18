package com.analytics.dhl;

import org.junit.Test;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;

import static org.junit.Assert.assertTrue;

import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.client.ClientUtil;
import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.core.har.HarEntry;
import net.lightbody.bmp.core.har.HarLog;
import net.lightbody.bmp.filters.RequestFilter;
import net.lightbody.bmp.util.HttpMessageContents;
import net.lightbody.bmp.util.HttpMessageInfo;

import org.openqa.selenium.By;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.Select;

import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
/**
 * 
 * @author dhl analytics
 * @since 2019-02-24
 */
public class FormEventsTest {
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
    public void shouldContainFormStartEventDimensionsForFormPages() {
    	final String url = "https://logistics-uat.dhl.com/au-en/home/our-divisions/global-forwarding/contact-us/general-inquiry.html";
    	
    	HashMap<String, String> map = new HashMap<>();
    	map.put("events", "event74");
    	map.put("v47", "General Inquiry");
    	map.put("v69", "Step 1");
    	
        proxy.newHar("shouldContainFormStartEventDimensionsForFormPages");
        driver.get(url);
        synchronized (driver) {
    		try {
				driver.wait(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
    	}
        Har har = proxy.getHar();
        HarLog harLog = har.getLog();
        List<HarEntry> tracking = harLog.getEntries()
        	.stream()
        	.filter(harEntry -> harEntry.getRequest().getUrl().contains("dhlcom.d3.sc.omtrdc.net/b/ss/"))
        	.collect(Collectors.toList());
        
    	boolean isOkay = HarRequestMetricExtractor.checkForTrackedValue(tracking, map);
        assertTrue(isOkay);
    }
    
    @Test
    public void shouldContainFormEndEventDimensionsForThankyouPages() {
    	final String url = "https://logistics-uat.dhl.com/au-en/home/our-divisions/global-forwarding/contact-us/general-inquiry/thank-you.html";
    	
    	HashMap<String, String> map = new HashMap<>();
    	map.put("events", "event99");
    	
        proxy.newHar("shouldContainFormEndEventDimensionsForThankyouPages");
        driver.get(url);
        synchronized (driver) {
    		try {
				driver.wait(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
    	}
        Har har = proxy.getHar();
        HarLog harLog = har.getLog();
        List<HarEntry> tracking = harLog.getEntries()
        	.stream()
        	.filter(harEntry -> harEntry.getRequest().getUrl().contains("dhlcom.d3.sc.omtrdc.net/b/ss/"))
        	.collect(Collectors.toList());
        
    	boolean isOkay = HarRequestMetricExtractor.checkForTrackedValue(tracking, map);
        assertTrue(isOkay);
    }
    
    @Test
    public void shouldContainFormSubmissionEventDimensionsForSubmission() {
    	final String url = "https://logistics-uat.dhl.com/au-en/home/our-divisions/global-forwarding/contact-us/general-inquiry.html";
    	
    	HashMap<String, String> map = new HashMap<>();
    	map.put("events", "event76");
    	
    	driver.get(url);
    	WebElement form = driver.findElement(By.tagName("form"));
    	List<WebElement> formElements = form.findElements(By.className("c-form--element-base"));
    	for(WebElement formElement : formElements) {
    		String tag = formElement.getTagName();
    		switch(tag) {
    			case "select":
    				Select select = new Select(formElement);
    				select.selectByIndex(1);
    				break;
    			case "input":
    				String type = formElement.getAttribute("type");
    				String name = formElement.getAttribute("name");
    				if(type.equals("checkbox") || name.isEmpty()) {
    					continue;
    				}
    				switch(name) {
    					case "email":
    						formElement.sendKeys("test@test.com");
    						break;
    					case "upload":
    						break;
    					default:
    						formElement.sendKeys("test");
    						break;
    				}
    				break;
    			case "textarea":
    				formElement.sendKeys("test");
    				break;
    			default:
    				break;
    		}
    	}
    	proxy.newHar("shouldContainFormSubmissionEventDimensionsForSubmission");
    	driver.findElement(By.tagName("button")).click();
    	synchronized (driver) {
    		try {
				driver.wait(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
    	}
    	Har har = proxy.getHar();
        HarLog harLog = har.getLog();
        List<HarEntry> tracking = harLog.getEntries()
        	.stream()
        	.filter(harEntry -> harEntry.getRequest().getUrl().contains("dhlcom.d3.sc.omtrdc.net/b/ss/"))
        	.collect(Collectors.toList());
    	
        boolean isOkay = HarRequestMetricExtractor.checkForTrackedValue(tracking, map);
        assertTrue(isOkay);
    }
}