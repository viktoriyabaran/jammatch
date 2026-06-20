package server.db.dao;

import server.db.ConnectionPool;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class RoundDao {
    private final ConnectionPool connectionPool;

    public RoundDao(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    public void insertRound(int gameId, String trackId, int correctUserId) throws InterruptedException, SQLException {
        String sql = "INSERT INTO rounds (game_id, track_id, correct_user_id) VALUES (?, ?, ?)";
        Connection conn = connectionPool.getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, gameId);
            pstmt.setString(2, trackId);
            pstmt.setInt(3, correctUserId);
            pstmt.executeUpdate();
        } finally {
            connectionPool.releaseConnection(conn);
        }
    }
}