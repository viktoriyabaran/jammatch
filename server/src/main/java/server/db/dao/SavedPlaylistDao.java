package server.db.dao;

import server.db.ConnectionPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SavedPlaylistDao {
    private final ConnectionPool connectionPool;

    public SavedPlaylistDao(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    public void save(int userId, String url, String name) throws InterruptedException, SQLException {
        String sql = "INSERT OR IGNORE INTO saved_playlists (user_id, url, name) VALUES (?, ?, ?)";
        Connection conn = connectionPool.getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, url);
            pstmt.setString(3, name);
            pstmt.executeUpdate();
        } finally {
            connectionPool.releaseConnection(conn);
        }
    }

    public List<SavedPlaylist> listForUser(int userId) throws InterruptedException, SQLException {
        String sql = "SELECT url, name FROM saved_playlists WHERE user_id = ? ORDER BY created_at DESC";
        Connection conn = connectionPool.getConnection();
        List<SavedPlaylist> playlists = new ArrayList<>();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    playlists.add(new SavedPlaylist(rs.getString("url"), rs.getString("name")));
                }
            }
        } finally {
            connectionPool.releaseConnection(conn);
        }
        return playlists;
    }
}
