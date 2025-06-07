package com.jouriroosjen.hardcoreSMPPlugin.database;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles database schema migrations using embedded SQL files.
 * Migration files should follow the naming convention: {@code V<version_number>__<description>.sql}
 * and be located in the {@code resources/migrations} folder.
 * <br/>
 * Example: {@code V1__create_deaths_table.sql}
 *
 * @author Jouri Roosjen
 * @version 1.0.0
 */
public class MigrationsManager {
    private final JavaPlugin plugin;
    private final Connection connection;

    /**
     * Represents a database migration file with a version number and its resource path.
     *
     * @param version The migration version number
     * @param resourcePath The path to the SQL file within the plugin's resources
     */
    private record Migration(int version, String resourcePath) {}

    /**
     * Constructs a new {@code MigrationsManager} instance.
     *
     * @param plugin The main plugin instance
     * @param connection The active database connection
     */
    public MigrationsManager(JavaPlugin plugin, Connection connection) {
        this.plugin = plugin;
        this.connection = connection;
    }

    /**
     * Applies any pending database migrations in version order.
     *
     * @throws SQLException If an error occurs during the migration process
     */
    public void migrate() throws SQLException {
        ensureSchemaTableExists();

        int currentVersion = getCurrentVersion();
        List<Migration> migrations = loadMigrations();

        for (Migration migration : migrations) {
            if (migration.version > currentVersion) {
                applyMigration(migration);
                setCurrentVersion(migration.version);
            }
        }

        plugin.getLogger().info("[DATABASE] Migrations migrated successfully.");
    }

    /**
     * Ensures the {@code schema_version} table exists and initializes it to version 0 if it does not.
     *
     * @throws SQLException If a database access error occurs
     */
    private void ensureSchemaTableExists() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                CREATE TABLE IF NOT EXISTS schema_version (
                    version INTEGER NOT NULL
                )
            """);
        }

        try (Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM schema_version");

            if (resultSet.next() && resultSet.getInt(1) == 0) {
                try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO schema_version (version) VALUES (0)")) {
                    preparedStatement.execute();
                }
            }
        }
    }

    /**
     * Loads all migration files from the resources folder and parses their version numbers.
     *
     * @return A list of {@link Migration} objects, sorted by version number
     */
    private List<Migration> loadMigrations() {
        try {
            List<String> resources = Arrays.asList(
                    "migrations/V1__create_deaths_table.sql"
            );

            Pattern pattern = Pattern.compile("V(\\d+)__.*\\.sql");

            List<Migration> migrations = new ArrayList<>();
            for (String resource : resources) {
                Matcher matcher = pattern.matcher(resource);
                if (matcher.find()) {
                    int version = Integer.parseInt(matcher.group(1));
                    migrations.add(new Migration(version, resource));
                }
            }

            migrations.sort(Comparator.comparingInt(migration -> migration.version));
            return migrations;
        } catch (Exception e) {
            throw new RuntimeException("[DATABASE] Failed to load migration list: ", e);
        }
    }

    /**
     * Applies a single migration by executing the SQL from the specified resource file.
     *
     * @param migration The {@link Migration} to apply
     * @throws SQLException If the migration fails or the file cannot be read
     */
    private void applyMigration(Migration migration) throws SQLException {
        try (InputStream input = plugin.getResource(migration.resourcePath)) {
            if (input == null) throw new RuntimeException("[DATABASE] Missing migration file: " + migration.resourcePath);

            String sql = new BufferedReader(new InputStreamReader(input))
                    .lines()
                    .reduce("", (a, b) -> a + "\n" + b);

            try (Statement statement = connection.createStatement()) {
                statement.execute(sql);
            }
        } catch (Exception e) {
            throw new SQLException("[DATABASE] Failed to apply migration: " + migration.resourcePath, e);
        }
    }

    /**
     * Retrieves the current schema version from the {@code schema_version} table.
     *
     * @return The current version number
     * @throws SQLException If a database access error occurs
     */
    private int getCurrentVersion() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery("SELECT version FROM schema_version");
            return resultSet.next() ? resultSet.getInt("version") : 0;
        }
    }

    /**
     * Updates the schema version in the {@code schema_version} table.
     *
     * @param version The new version number to set
     * @throws SQLException If a database access error occurs
     */
    private void setCurrentVersion(int version) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("UPDATE schema_version SET version = ?")) {
            preparedStatement.setInt(1, version);
            preparedStatement.executeUpdate();
        }
    }
}
