package com.jwt.server.http;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsonUtils {

    public static String extractValue(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\"");
        Matcher matcher = pattern.matcher(json);

        if (matcher.find()) {
            int start = matcher.end();
            StringBuilder value = new StringBuilder();
            for (int i = start; i < json.length(); i++) {
                char c = json.charAt(i);
                if (c == '\\' && i + 1 < json.length()) {
                    value.append(json.charAt(i + 1));
                    i++;
                } else if (c == '"') {
                    return value.toString();
                } else {
                    value.append(c);
                }
            }
            return value.toString();
        }
        return null;
    }

}
