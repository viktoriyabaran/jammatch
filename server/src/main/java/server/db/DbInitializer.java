package server.db;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.Statement;
import java.util.stream.Collectors;

public class DbInitializer {
    private final ConnectionPool connectionPool;

    public DbInitializer(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    public void initialize() {
        try {
            Connection conn = connectionPool.getConnection();
            try (Statement stmt = conn.createStatement()) {
                String sql = loadSchemaSql();
                String[] statements = sql.split(";");
                for (String s : statements) {
                    if (!s.trim().isEmpty()) {
                        stmt.execute(s);
                    }
                }
            } finally {
                connectionPool.releaseConnection(conn);
            }
        } catch (Exception e) {
            throw new RuntimeException("DB setup failed", e);
        }
    }

    private String loadSchemaSql() {
        InputStream is = getClass().getResourceAsStream("/schema.sql");
        if (is == null) {
            throw new RuntimeException("No schema.sql found");
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            throw new RuntimeException("Failed to read schema.sql", e);
        }
    }
}