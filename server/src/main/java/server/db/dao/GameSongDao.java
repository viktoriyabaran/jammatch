package server.db.dao;

import server.db.ConnectionPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class GameSongDao {
    private final ConnectionPool connectionPool;

    public GameSongDao(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    public void addSongs(int gameId, int userId, List<String> videoIds) throws InterruptedException, SQLException {
        String sql = "INSERT OR IGNORE INTO game_songs (game_id, user_id, video_id) VALUES (?, ?, ?)";
        Connection conn = connectionPool.getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (String videoId : videoIds) {
                pstmt.setInt(1, gameId);
                pstmt.setInt(2, userId);
                pstmt.setString(3, videoId);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        } finally {
            connectionPool.releaseConnection(conn);
        }
    }

    public Optional<RoundSong> pickRandomUnplayed(int gameId) throws InterruptedException, SQLException {
        String sql = """
                SELECT gs.video_id, gs.user_id, sc.title, sc.artist, sc.cover_url
                FROM game_songs gs
                JOIN song_cache sc ON sc.video_id = gs.video_id
                LEFT JOIN banned_artists b ON b.name = sc.artist COLLATE NOCASE
                WHERE gs.game_id = ?
                  AND b.id IS NULL
                  AND gs.video_id NOT IN (SELECT track_id FROM rounds WHERE game_id = ?)
                GROUP BY gs.video_id
                HAVING COUNT(DISTINCT gs.user_id) = 1
                ORDER BY RANDOM()
                LIMIT 1
                """;
        Connection conn = connectionPool.getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, gameId);
            pstmt.setInt(2, gameId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new RoundSong(
                        rs.getString("video_id"),
                        rs.getInt("user_id"),
                        rs.getString("title"),
                        rs.getString("artist"),
                        rs.getString("cover_url")));
            }
        } finally {
            connectionPool.releaseConnection(conn);
        }
    }
}
