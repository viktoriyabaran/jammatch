package server.db.dao;

import server.db.ConnectionPool;
import server.external.model.CachedSong;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class SongCacheDao {
    private final ConnectionPool connectionPool;

    public SongCacheDao(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    public Optional<CachedSong> find(String videoId) throws InterruptedException, SQLException {
        String sql = "SELECT title, artist, cover_url, matched FROM song_cache WHERE video_id = ?";
        Connection conn = connectionPool.getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, videoId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new CachedSong(
                        videoId,
                        rs.getString("title"),
                        rs.getString("artist"),
                        rs.getString("cover_url"),
                        rs.getInt("matched") == 1));
            }
        } finally {
            connectionPool.releaseConnection(conn);
        }
    }

    public void save(CachedSong song) throws InterruptedException, SQLException {
        String sql = "INSERT OR REPLACE INTO song_cache (video_id, title, artist, cover_url, matched) VALUES (?, ?, ?, ?, ?)";
        Connection conn = connectionPool.getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, song.videoId());
            pstmt.setString(2, song.title());
            pstmt.setString(3, song.artist());
            pstmt.setString(4, song.coverUrl());
            pstmt.setInt(5, song.matched() ? 1 : 0);
            pstmt.executeUpdate();
        } finally {
            connectionPool.releaseConnection(conn);
        }
    }
}
