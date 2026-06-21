package server.external;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

public class HttpJson {
    private final HttpClient client;

    public HttpJson() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public JsonObject get(String url, Map<String, String> headers) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .GET();

        for (Map.Entry<String, String> h : headers.entrySet()) {
            builder.header(h.getKey(), h.getValue());
        }

        return send(builder.build());
    }

    public JsonObject postForm(String url, Map<String, String> form, Map<String, String> headers) throws IOException, InterruptedException {
        String body = encodeForm(form);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body));

        for (Map.Entry<String, String> h : headers.entrySet()) {
            builder.header(h.getKey(), h.getValue());
        }

        return send(builder.build());
    }

    public static String query(Map<String, String> params) {
        return "?" + encodeForm(params);
    }

    private static String encodeForm(Map<String, String> params) {
        return params.entrySet().stream()
                .map(p -> encode(p.getKey()) + "=" + encode(p.getValue()))
                .collect(Collectors.joining("&"));
    }

    private static String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private JsonObject send(HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        int status = response.statusCode();
        String body = response.body();

        if (status < 200 || status >= 300) {
            String snippet = body.length() > 300 ? body.substring(0, 300) : body;
            throw new IOException("HTTP " + status + " for " + request.uri() + ": " + snippet);
        }

        return JsonParser.parseString(body).getAsJsonObject();
    }
}
