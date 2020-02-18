package com.analytics.dhl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class DataLayerValidator {
	public static void main(String[] args) {
		String jsonString = "{\"page\":{\"pageInfo\":{\"pageName\":\"EasyPost | DHL |  Gambia | EN\",\"pageNamey\":\"\",\"language\":\"en-GM\",\"publisher\":\"Core\"},\"category\":{\"primaryCategory\":\"Sales Product\",\"subCategory1\":\"Detail\",\"subCategory2\":\"NA\",\"contentType\":\"NA\"},\"attributes\":{\"campaign\":\"NA\"}}}";
		List<String> errors = validate(jsonString, new String[] {"page.pageInfo.pageName", "page.pageInfo.pageNamey", "page.hund"});
		if(errors != null) {
			System.out.println(errors.stream().collect(Collectors.joining(",\r\n")));
		}
	}
	
	private static String checkKey(String key, JsonObject obj) {
		JsonObject nobj = null;
		JsonElement element = null;
		String[] tree = key.split("\\.");
		
		if(!obj.has(tree[0])) {
			return String.format("%s is missing in dataLayer", tree[0]);
		} else {
			element = obj.get(tree[0]);
			try {
				nobj = obj.get(tree[0]).getAsJsonObject();
			} catch(IllegalStateException e) {}
		}
		
		if(tree.length > 1) {
			return checkKey(String.join(".", Arrays.copyOfRange(tree, 1, tree.length)), nobj);
		} else {
			if(element.toString().replaceAll("\"", "").isEmpty()) {
				return String.format("%s has an empty value", tree[0]);
			}
			if(element.toString().replaceAll("\"", "").equals("NA")) {
				return String.format("%s is NA", tree[0]);
			}
		}
		
		return null;
	}
	
	public static List<String> validate(String dataLayerString, String[] keysToCheck) {
		List<String> errors = new ArrayList<>();
		JsonObject obj = new JsonParser().parse(dataLayerString).getAsJsonObject();
		
		for(String key : keysToCheck) {
			String check = checkKey(key, obj);
			if(check != null) {
				errors.add(check);
			}
		}
		
		return errors.size() > 0 ? errors : null;
	}
}
