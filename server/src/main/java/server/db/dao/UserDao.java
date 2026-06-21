package server.db.dao;

import server.db.ConnectionPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public class UserDao {
    private final ConnectionPool connectionPool;

    public UserDao(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    public int insertUser(String nickname) throws InterruptedException, SQLException {
        String sql = "INSERT INTO users (nickname) VALUES (?)";
        Connection conn = connectionPool.getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, nickname);
            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            throw new SQLException("Insert failed, no ID found");
        } finally {
            connectionPool.releaseConnection(conn);
        }
    }

    public int findOrCreateByToken(String clientToken, String nickname) throws InterruptedException, SQLException {
        Connection conn = connectionPool.getConnection();

        try {
            try (PreparedStatement insert = conn.prepareStatement(
                    "INSERT OR IGNORE INTO users (client_token, nickname) VALUES (?, ?)")) {
                insert.setString(1, clientToken);
                insert.setString(2, nickname);
                insert.executeUpdate();
            }
            try (PreparedStatement update = conn.prepareStatement(
                    "UPDATE users SET nickname = ? WHERE client_token = ?")) {
                update.setString(1, nickname);
                update.setString(2, clientToken);
                update.executeUpdate();
            }
            try (PreparedStatement select = conn.prepareStatement(
                    "SELECT id FROM users WHERE client_token = ?")) {
                select.setString(1, clientToken);
                try (ResultSet rs = select.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("id");
                    }
                }
            }
            throw new SQLException("Find-or-create failed for token");
        } finally {
            connectionPool.releaseConnection(conn);
        }
    }

    public Optional<String> getUserNickname(int id) throws InterruptedException, SQLException {
        String sql = "SELECT nickname FROM users WHERE id = ?";
        Connection conn = connectionPool.getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rs.getString("nickname"));
                }
            }
        } finally {
            connectionPool.releaseConnection(conn);
        }
        return Optional.empty();
    }
}