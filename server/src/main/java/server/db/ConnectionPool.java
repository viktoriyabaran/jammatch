package server.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class ConnectionPool {
    private static final String URL = "jdbc:sqlite:jammatch.db";
    private static final int POOL_SIZE = 10;
    private final BlockingQueue<Connection> connectionQueue;

    public ConnectionPool() {
        connectionQueue = new ArrayBlockingQueue<>(POOL_SIZE);
        initializePool();
    }

    private void initializePool() {
        try {
            for (int i = 0; i < POOL_SIZE; i++) {
                Connection connection = DriverManager.getConnection(URL);
                connectionQueue.add(connection);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to init DB connections", e);
        }
    }

    public Connection getConnection() throws InterruptedException {
        return connectionQueue.take();
    }

    public void releaseConnection(Connection connection) {
        if (connection != null) {
            connectionQueue.offer(connection);
        }
    }
}
