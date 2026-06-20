package server.db.dao;

import server.db.ConnectionPool;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BannedArtistDao {
    private final ConnectionPool connectionPool;

    public BannedArtistDao(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    public boolean isArtistBanned(String artistName) throws InterruptedException, SQLException {
        String sql = "SELECT 1 FROM banned_artists WHERE name COLLATE NOCASE = ?";
        Connection conn = connectionPool.getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, artistName);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next(); // returns true if we found a match
            }
        } finally {
            connectionPool.releaseConnection(conn);
        }
    }
}