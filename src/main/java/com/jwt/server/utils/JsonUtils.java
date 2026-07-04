package com.jwt.server.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsonUtils {
    // Extracts the value associated with the specified key from a JSON string.
    public static String extractValue(String json, String key) {
        String search = "\"" + key + "\"\\s*:\\s*\"";
        Pattern pattern = Pattern.compile(search);
        Matcher matcher = pattern.matcher(json);
        
        if (matcher.find()) {
            int start = matcher.end();
            int end = json.indexOf("\"", start);
            return json.substring(start, end);
        }
        return null;
    }

    public static String extractValueN(String json, String key) {
        // Search for "key": 
        String search = "\"" + key + "\"\\s*:\\s*\"?";
        Pattern pattern = Pattern.compile(search);
        Matcher matcher = pattern.matcher(json);
        
        if (matcher.find()) {
            int start = matcher.end();
            
            // Check if value is in quotes
            if (json.charAt(start - 1) == '"') {
                // Quoted value: "value": "100"
                int end = json.indexOf("\"", start);
                return json.substring(start, end);
            } else {
                // Unquoted value: "value": 100
                int end = start;
                while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.' || json.charAt(end) == '-')) {
                    end++;
                }
                return json.substring(start, end);
            }
        }
        return null;
    }
}