package com.jouriroosjen.hardcoreSMPPlugin.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StartCountdownCommand implements CommandExecutor {
    private final Connection connection;

    /**
     * Construct a new {@code StartCountdownCommand} instance.
     *
     * @param connection The database connection instance.
     */
    public StartCountdownCommand(Connection connection) {
        this.connection = connection;
    }

    /**
     * Executes the start countdown command logic when a OP runs it.
     *
     * @param sender  The source of the command
     * @param command The command that was executed
     * @param label   The alias used
     * @param args    The command arguments
     * @return {@code true} if the command was handled, {@code false} otherwise
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!sender.isOp()) {
            sender.sendMessage(Component.text("Only OP's are allowed to run this command!", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0 || args.length > 4) return false;

        try {
            Duration duration = parseDuration(String.join(" ", args));
            Instant endTime = Instant.now().plus(duration);

            String endTimeString = endTime.toString();
            createCountdown(endTimeString);

            sender.sendMessage("Countdown started! Ends at: " + formatForDisplay(endTime));
        } catch (IllegalArgumentException e) {
            sender.sendMessage("Invalid duration format! Use: 1h 30m, 2d, etc.");
        }

        return true;
    }

    /**
     * Parse the remaining time input.
     *
     * @param input The user input.
     * @return The duration of the timer.
     */
    private Duration parseDuration(String input) {
        Duration total = Duration.ZERO;
        Pattern pattern = Pattern.compile("(\\d+)([dhms])");
        Matcher matcher = pattern.matcher(input.toLowerCase());

        while (matcher.find()) {
            int amount = Integer.parseInt(matcher.group(1));
            String unit = matcher.group(2);

            switch (unit) {
                case "d" -> total = total.plusDays(amount);
                case "h" -> total = total.plusHours(amount);
                case "m" -> total = total.plusMinutes(amount);
                case "s" -> total = total.plusSeconds(amount);
            }
        }

        if (total.isZero())
            throw new IllegalArgumentException("No valid duration found");

        return total;
    }

    /**
     * Format the end time to a human-readable string.
     *
     * @param instant The instant at which the countdown will end.
     * @return A human-readable string of the end time.
     */
    private String formatForDisplay(Instant instant) {
        return instant.atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * Create a countdown in the database.
     *
     * @param endTime The date and time the countdown should end.
     */
    private void createCountdown(String endTime) {
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
