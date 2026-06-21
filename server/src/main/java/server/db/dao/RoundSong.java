package server.db.dao;

public record RoundSong(String videoId, int correctUserId, String title, String artist, String coverUrl) {
}
