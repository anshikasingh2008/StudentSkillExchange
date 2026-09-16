package com.skillexchange.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tiny hand-rolled JSON writer and form-body parser so the web layer
 * has zero extra dependencies (no Gson/Jackson) - keeps the whole
 * project buildable with just `javac`. Only needs to go Java-object
 * -> JSON (responses); incoming request bodies are parsed as plain
 * application/x-www-form-urlencoded, which is trivial to parse by
 * hand and keeps app.js simple (URLSearchParams on the client side).
 */
public final class JsonUtil {

    private JsonUtil() {
    }

    public static String toJson(Object value) {
        if (value == null) return "null";
        if (value instanceof String s) return quote(s);
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        if (value instanceof Map<?, ?> map) {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (!first) sb.append(",");
                first = false;
                sb.append(quote(String.valueOf(e.getKey()))).append(":").append(toJson(e.getValue()));
            }
            return sb.append("}").toString();
        }
        if (value instanceof List<?> list) {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object o : list) {
                if (!first) sb.append(",");
                first = false;
                sb.append(toJson(o));
            }
            return sb.append("]").toString();
        }
        return quote(value.toString());
    }

    public static Map<String, Object> obj() {
        return new LinkedHashMap<>();
    }

    private static String quote(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        return sb.append("\"").toString();
    }

    /** Reads the full request body as a UTF-8 string. */
    public static String readBody(InputStream is) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        is.transferTo(buf);
        return buf.toString(StandardCharsets.UTF_8);
    }

    /** Parses application/x-www-form-urlencoded bodies (or query strings) into a map. */
    public static Map<String, String> parseForm(String body) {
        Map<String, String> result = new LinkedHashMap<>();
        if (body == null || body.isBlank()) return result;
        for (String pair : body.split("&")) {
            if (pair.isBlank()) continue;
            String[] kv = pair.split("=", 2);
            String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
            String val = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
            result.put(key, val);
        }
        return result;
    }
}
