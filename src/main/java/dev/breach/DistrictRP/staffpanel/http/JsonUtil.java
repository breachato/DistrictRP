package dev.breach.DistrictRP.staffpanel.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class JsonUtil {

    public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private JsonUtil() {}

    public static String readBody(HttpExchange ex) throws IOException {
        try (InputStream is = ex.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public static void writeJson(HttpExchange ex, int status, Object payload) throws IOException {
        byte[] bytes = GSON.toJson(payload).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.sendResponseHeaders(status, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.getResponseBody().close();
    }

    public static void writeError(HttpExchange ex, int status, String message) throws IOException {
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("success", false);
        m.put("error", message);
        writeJson(ex, status, m);
    }
}