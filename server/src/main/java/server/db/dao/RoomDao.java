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

    public int createRoom(String roomCode, String roomName, int hostId, int maxPlayers, int totalRounds,
            int roundDurationSec) throws InterruptedException, SQLException {
        String sql = "INSERT INTO rooms (room_code, room_name, host_id, max_players, total_rounds, round_duration_sec) VALUES (?, ?, ?, ?, ?, ?)";
        Connection conn = connectionPool.getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, roomCode);
            pstmt.setString(2, roomName);
            pstmt.setInt(3, hostId);
            pstmt.setInt(4, maxPlayers);
            pstmt.setInt(5, totalRounds);
            pstmt.setInt(6, roundDurationSec);
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

    public void closeRoom(String roomCode) throws InterruptedException, SQLException {
        String sql = "DELETE FROM rooms WHERE room_code = ?";
        Connection conn = connectionPool.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, roomCode);
            pstmt.executeUpdate();
        } finally {
            connectionPool.releaseConnection(conn);
        }
    }
}