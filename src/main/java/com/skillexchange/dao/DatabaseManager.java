package com.skillexchange.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Singleton that owns the single JDBC Connection used throughout the
 * app and creates the schema on first run.
 *
 * Uses SQLite for the demo (zero-config, file-based, no server needed)
 * so the project runs anywhere out of the box. Swapping to MySQL/
 * PostgreSQL for a "real" deployment only requires changing the
 * DB_URL below and the JDBC driver on the classpath - nothing in the
 * DAO classes needs to change since they only use java.sql types.
 *
 * Demonstrates the Singleton design pattern (Java OOP: Other Types of
 * Classes) and the JDBC connection-setup steps from the syllabus:
 * driver loading, connecting, submitting statements.
 */
public final class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:data/skill_exchange.db";
    private static DatabaseManager instance;
    private final Connection connection;

    private DatabaseManager() throws SQLException {
        // With modern JDBC (4.0+) the driver is auto-loaded via
        // META-INF/services, but we load it explicitly here to keep
        // the "Class.forName" step visible for the course syllabus.
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC driver not found on classpath", e);
        }
        connection = DriverManager.getConnection(DB_URL);
        initializeSchema();
    }

    public static synchronized DatabaseManager getInstance() throws SQLException {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }

    private void initializeSchema() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY,
                    name TEXT NOT NULL,
                    email TEXT NOT NULL UNIQUE,
                    branch TEXT,
                    year INTEGER,
                    credits INTEGER NOT NULL DEFAULT 0,
                    rating_total REAL NOT NULL DEFAULT 0,
                    rating_count INTEGER NOT NULL DEFAULT 0
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS skills (
                    id INTEGER PRIMARY KEY,
                    name TEXT NOT NULL,
                    category TEXT NOT NULL,
                    description TEXT
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS skill_offers (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    skill_id INTEGER NOT NULL,
                    level TEXT NOT NULL,
                    FOREIGN KEY (user_id) REFERENCES users(id),
                    FOREIGN KEY (skill_id) REFERENCES skills(id)
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS skill_wants (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    skill_id INTEGER NOT NULL,
                    FOREIGN KEY (user_id) REFERENCES users(id),
                    FOREIGN KEY (skill_id) REFERENCES skills(id)
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS exchange_requests (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    requester_id INTEGER NOT NULL,
                    provider_id INTEGER NOT NULL,
                    skill_id INTEGER NOT NULL,
                    credits_offered INTEGER NOT NULL,
                    status TEXT NOT NULL,
                    created_at TEXT NOT NULL,
                    FOREIGN KEY (requester_id) REFERENCES users(id),
                    FOREIGN KEY (provider_id) REFERENCES users(id),
                    FOREIGN KEY (skill_id) REFERENCES skills(id)
                )
            """);
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("Error closing database connection: " + e.getMessage());
        }
    }
}
