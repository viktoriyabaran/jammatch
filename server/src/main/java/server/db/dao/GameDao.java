package server.db.dao;

import server.db.ConnectionPool;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class GameDao {
    private final ConnectionPool connectionPool;

    public GameDao(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    public int createGame(int roomId) throws InterruptedException, SQLException {
        String sql = "INSERT INTO games (room_id, status) VALUES (?, 'IN_PROGRESS')";
        Connection conn = connectionPool.getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, roomId);
            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            throw new SQLException("Insert failed, didn't get a game ID back");
        } finally {
            connectionPool.releaseConnection(conn);
        }
    }

    public void updateGameStatus(int gameId, String status) throws InterruptedException, SQLException {
        String sql = "UPDATE games SET status = ? WHERE id = ?";
        Connection conn = connectionPool.getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setInt(2, gameId);
            pstmt.executeUpdate();
        } finally {
            connectionPool.releaseConnection(conn);
        }
    }
}