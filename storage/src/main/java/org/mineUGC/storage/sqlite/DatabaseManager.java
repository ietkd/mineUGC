package org.mineUGC.storage.sqlite;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

public class DatabaseManager implements AutoCloseable {
    private Connection connection;
    private final File dbFile;
    private final Logger logger;

    public DatabaseManager(File dbFile, Logger logger) {
        this.dbFile = dbFile;
        this.logger = logger;
    }

    public void initialize() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS player_cooldowns (
                    player_uuid TEXT NOT NULL,
                    item_id TEXT NOT NULL,
                    ability_key TEXT NOT NULL,
                    expires_at INTEGER NOT NULL,
                    PRIMARY KEY (player_uuid, item_id, ability_key)
                )
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS player_data (
                    player_uuid TEXT PRIMARY KEY,
                    unlocked_items TEXT NOT NULL DEFAULT '[]'
                )
            """);
        }
        logger.info("Database initialized: " + dbFile.getName());
    }

    public Connection getConnection() {
        return connection;
    }

    @Override
    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                logger.warning("Failed to close database: " + e.getMessage());
            }
        }
    }
}
