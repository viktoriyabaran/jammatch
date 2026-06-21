package server.external;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import server.external.model.ResolvedTrack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class YouTubeClient {
    private static final String BASE = "https://www.googleapis.com/youtube/v3";
    private static final int MAX_TRACKS = 200;

    private final HttpJson http;
    private final String apiKey;

    public YouTubeClient(HttpJson http, String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("YOUTUBE_API_KEY not set (env not loaded?)");
        }
        this.http = http;
        this.apiKey = apiKey;
    }

    public static void main(String[] args) throws Exception {
        YouTubeClient yt = new YouTubeClient(new HttpJson(), System.getenv("YOUTUBE_API_KEY"));
        yt.fetchTracks("https://music.youtube.com/playlist?list=PLCwmfWC-ZFHQjhHVZGyYHFg-Ao_onaLU1").forEach(System.out::println);
    }

    public List<ResolvedTrack> fetchTracks(String playlistLink) throws IOException, InterruptedException {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("part", "snippet,contentDetails");
        params.put("playlistId", parseLink(playlistLink));
        params.put("key", apiKey);

        List<ResolvedTrack> tracks = new ArrayList<>();
        List<String> trackIds;
        String pageToken = null;
        boolean morePages = true;

        while (morePages && tracks.size() < MAX_TRACKS) {
            params.put("maxResults", String.valueOf(Math.min(50, MAX_TRACKS - tracks.size())));
            if (pageToken != null) {
                params.put("pageToken", pageToken);
            }

            JsonObject json = http.get(BASE + "/playlistItems" + HttpJson.query(params), Map.of());
            JsonArray items = json.getAsJsonArray("items");

            for (JsonElement element : items) {
                JsonObject item = element.getAsJsonObject();
                JsonObject snippet = item.getAsJsonObject("snippet");
                JsonObject contentDetails = item.getAsJsonObject("contentDetails");
                var author = str(snippet, "videoOwnerChannelTitle") != null ? str(snippet, "videoOwnerChannelTitle") : str(snippet, "channelTitle");
                tracks.add(new ResolvedTrack(
                        str(contentDetails, "videoId"),
                        str(snippet, "title"),
                        author
                ));
            }

            pageToken = json.has("nextPageToken") ? json.get("nextPageToken").getAsString() : null;
            morePages = pageToken != null;
        }

        trackIds = tracks
                .stream()
                .map(ResolvedTrack::videoId)
                .toList();

        Map<String, Boolean> videoStatusMap = checkEmbeddableVideos(trackIds);

        return tracks.stream()
                .filter(t -> videoStatusMap.getOrDefault(t.videoId(), false))
                .toList();
    }

    private String parseLink(String playlistLink) {
        int index = playlistLink.indexOf("list=");
        if (index < 0) {
            return playlistLink;
        }
        String rest = playlistLink.substring(index + 5);
        int amp = rest.indexOf('&');
        return amp < 0 ? rest : rest.substring(0, amp);
    }

    private static String str(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return el == null || el.isJsonNull() ? null : el.getAsString();
    }

    private LinkedHashMap<String, Boolean> checkEmbeddableVideos(List<String> trackIds) throws IOException, InterruptedException {
        if (trackIds.isEmpty())
            return new LinkedHashMap<>();

        LinkedHashMap<String, Boolean> results = new LinkedHashMap<>();

        for (int i = 0; i < trackIds.size(); i+=50) {
            List<String> batch = trackIds.subList(i, Math.min(i + 50, trackIds.size()));

            Map<String, String> params = new LinkedHashMap<>();
            params.put("part", "status");
            params.put("id", String.join(",", batch));
            params.put("key", apiKey);

            JsonObject json = http.get(BASE + "/videos" + HttpJson.query(params), Map.of());
            JsonArray items = json.getAsJsonArray("items");

            for (JsonElement element: items) {
                JsonObject item = element.getAsJsonObject();
                String videoId = str(item, "id");
                JsonElement isEmbeddableStr = item.getAsJsonObject("status").getAsJsonPrimitive("embeddable");
                Boolean isEmbeddable = isEmbeddableStr.getAsBoolean();
                results.put(videoId, isEmbeddable);
            }
        }

        return results;
    }
}