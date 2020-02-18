package com.analytics.dhl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.core.har.HarEntry;
import net.lightbody.bmp.core.har.HarLog;
import net.lightbody.bmp.core.har.HarNameValuePair;
import net.lightbody.bmp.core.har.HarRequest;

public class HarRequestMetricExtractor {
	public static boolean checkForTrackedValue(Har har, Map<String, String> map) {
		return checkForTrackedValue(getEntries(har), map);
	}
	
	public static boolean checkForTrackedValue(List<HarEntry> harEntries, Map<String, String> map) {
		for(HarEntry harEntry : harEntries) {
			HarRequest harRequest = harEntry.getRequest();
			List<HarNameValuePair> query =  harRequest.getQueryString();
			Map<String, String> matches = query.stream()
												.filter(key -> map.keySet().contains(key.getName()))
												.collect(Collectors.toMap(key -> key.getName(), key -> key.getValue()));
			if(map.size() == matches.size()) {
				int i = map.size();
				for(String key : map.keySet()) {
					List<String> vals = Arrays.asList(matches.get(key).split(","));
					List<String> mapVals = Arrays.asList(map.get(key).split(","));
					if((map.get(key).equals("*") || vals.containsAll(mapVals)) && --i == 0) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	private static List<HarEntry> getEntries(Har har) {
		HarLog harLog = har.getLog();
        List<HarEntry> tracking = harLog.getEntries()
        	.stream()
        	.filter(harEntry -> harEntry.getRequest().getUrl().contains("dhlglobalrolloutprod"))
        	.collect(Collectors.toList());
        return tracking;
	}
}
