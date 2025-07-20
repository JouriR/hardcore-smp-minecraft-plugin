package com.jouriroosjen.hardcoreSMPPlugin.managers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

/**
 * Manages event countdowns.
 *
 * @author Jouri Roosjen
 * @version 1.0.0
 */
public class CountdownManager {
    private final Connection connection;

    /**
     * Constructs a new {@code CountdownManager} instance.
     *
     * @param connection The active database connection.
     */
    public CountdownManager(Connection connection) {
        this.connection = connection;
    }

    /**
     * Create a countdown in the database.
     *
     * @param endTime The date and time the countdown should end.
     */
    public void createCountdown(String endTime) {
        CompletableFuture.runAsync(() -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO countdowns (end_at, created_at, updated_at)
                    VALUES (?, datetime('now'), datetime('now'))
                    """)) {
                statement.setString(1, endTime);
                statement.execute();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }
}
