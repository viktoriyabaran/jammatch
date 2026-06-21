package server.external;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import server.external.model.AlbumCover;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public class SpotifyClient {
    private static final String TOKEN_URL = "https://accounts.spotify.com/api/token";
    private static final String API_BASE = "https://api.spotify.com/v1";
    private static final double STRONG_MATCH = 0.9;
    private static final double MATCH_THRESHOLD = 0.5;

    private final HttpJson http;
    private final String clientId;
    private final String clientSecret;

    private String cachedToken;
    private long expiresAt;

    public SpotifyClient(HttpJson http, String clientId, String clientSecret) {
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalStateException("SPOTIFY_CLIENT_ID/SECRET not set (env not loaded?)");
        }
        this.http = http;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public AlbumCover findAlbumCover(String title, String channel) throws IOException, InterruptedException
    {
        String[] split = AlbumCoverMatcher.splitTitle(title);
        String artistFromTitle = split[0];
        String cleanSong = split[1];
        String cleanArtist = AlbumCoverMatcher.cleanChannel(channel);
        String targetArtist = !artistFromTitle.isBlank() ? artistFromTitle : cleanArtist;

        String token = accessToken();
        JsonObject bestTrack = null;
        double bestScore = 0.0;

        for (String query : buildQueries(cleanSong, artistFromTitle, cleanArtist, title)) {
            if (query.isBlank()) {
                continue;
            }

            String url = API_BASE + "/search" + HttpJson.query(Map.of(
                    "q", query, "type", "track", "limit", "10"));
            JsonArray items = http.get(url, Map.of("Authorization", "Bearer " + token))
                    .getAsJsonObject("tracks").getAsJsonArray("items");

            for (JsonElement element : items) {
                JsonObject track = element.getAsJsonObject();
                double score = AlbumCoverMatcher.matchScore(track.get("name").getAsString(), artistNames(track), cleanSong, targetArtist);
                if (score > bestScore) {
                    bestScore = score;
                    bestTrack = track;
                }
            }

            if (bestScore >= STRONG_MATCH) {
                break;
            }
        }

        if (bestTrack == null || bestScore < MATCH_THRESHOLD) {
            return null;
        }
        return toAlbumCover(bestTrack);
    }

    private static List<String> buildQueries(String song, String artistFromTitle, String cleanArtist, String rawTitle) {
        List<String> queries = new ArrayList<>();
        if (!artistFromTitle.isBlank()) {
            queries.add("track:\"" + song + "\" artist:\"" + artistFromTitle + "\"");
            queries.add(song + " " + artistFromTitle);
        }
        if (!cleanArtist.isBlank()) {
            queries.add("track:\"" + song + "\" artist:\"" + cleanArtist + "\"");
            queries.add(song + " " + cleanArtist);
        }
        queries.add(song);
        queries.add(rawTitle);
        return queries;
    }

    private static List<String> artistNames(JsonObject track) {
        List<String> names = new ArrayList<>();
        for (JsonElement a : track.getAsJsonArray("artists")) {
            names.add(a.getAsJsonObject().get("name").getAsString());
        }
        return names;
    }

    private static AlbumCover toAlbumCover(JsonObject track) {
        String trackName = track.get("name").getAsString();
        String artistName = track.getAsJsonArray("artists").get(0).getAsJsonObject().get("name").getAsString();
        JsonArray images = track.getAsJsonObject("album").getAsJsonArray("images");
        String coverUrl = images.isEmpty() ? null : images.get(0).getAsJsonObject().get("url").getAsString();
        return new AlbumCover(coverUrl, trackName, artistName);
    }

    private synchronized String accessToken() throws IOException, InterruptedException {
        long now = System.currentTimeMillis();
        if (cachedToken != null && now < expiresAt - 60_000) {
            return cachedToken;
        }

        String basic = Base64.getEncoder()
                .encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));

        JsonObject json = http.postForm(TOKEN_URL,
                Map.of("grant_type", "client_credentials"),
                Map.of("Authorization", "Basic " + basic));

        cachedToken = json.get("access_token").getAsString();
        long expiresIn = json.has("expires_in") ? json.get("expires_in").getAsLong() : 3600;
        expiresAt = now + expiresIn * 1000;
        return cachedToken;
    }

    public static void main(String[] args) throws Exception {
        SpotifyClient sp = new SpotifyClient(new HttpJson(),
                System.getenv("SPOTIFY_CLIENT_ID"), System.getenv("SPOTIFY_CLIENT_SECRET"));
        System.out.println(sp.findAlbumCover("Love Me or Hate Me", "Song Soo Woo"));
    }
}