package server.external.model;

public record CachedSong(String videoId, String title, String artist, String coverUrl, boolean matched) {
}
