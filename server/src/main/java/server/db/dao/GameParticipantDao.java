package server.db.dao;

import server.db.ConnectionPool;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class GameParticipantDao {
    private final ConnectionPool connectionPool;

    public GameParticipantDao(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    public void addParticipant(int gameId, int userId, String playlistUrl) throws InterruptedException, SQLException {
        String sql = "INSERT INTO game_participants (game_id, user_id, playlist_url, score) VALUES (?, ?, ?, 0)";
        Connection conn = connectionPool.getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, gameId);
            pstmt.setInt(2, userId);
            pstmt.setString(3, playlistUrl);
            pstmt.executeUpdate();
        } finally {
            connectionPool.releaseConnection(conn);
        }
    }

    public void addScore(int gameId, int userId, int pointsToAdd) throws InterruptedException, SQLException {
        String sql = "UPDATE game_participants SET score = score + ? WHERE game_id = ? AND user_id = ?";
        Connection conn = connectionPool.getConnection();

        try (PreparedStatement pstmt = prepareStatement(conn, sql)) {
            pstmt.setInt(1, pointsToAdd);
            pstmt.setInt(2, gameId);
            pstmt.setInt(3, userId);
            pstmt.executeUpdate();
        } finally {
            connectionPool.releaseConnection(conn);
        }
    }

    private PreparedStatement prepareStatement(Connection conn, String sql) throws SQLException {
        return conn.prepareStatement(sql);
    }
}