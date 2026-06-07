package server.db.dao;

import server.db.ConnectionPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public class RoomDao {
    private final ConnectionPool connectionPool;

    public RoomDao(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    public int createRoom(String roomCode, int hostId, int totalRounds, int roundDurationSec)
            throws InterruptedException, SQLException {
        String sql = "INSERT INTO rooms (room_code, host_id, total_rounds, round_duration_sec) VALUES (?, ?, ?, ?)";
        Connection conn = connectionPool.getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, roomCode);
            pstmt.setInt(2, hostId);
            pstmt.setInt(3, totalRounds);
            pstmt.setInt(4, roundDurationSec);
            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            throw new SQLException("Insert failed, didn't get a room ID back");
        } finally {
            connectionPool.releaseConnection(conn);
        }
    }

    public Optional<Integer> getRoomIdByCode(String roomCode) throws InterruptedException, SQLException {
        String sql = "SELECT id FROM rooms WHERE room_code = ?";
        Connection conn = connectionPool.getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, roomCode);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rs.getInt("id"));
                }
            }
        } finally {
            connectionPool.releaseConnection(conn);
        }
        return Optional.empty();
    }
}