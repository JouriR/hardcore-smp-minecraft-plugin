package com.jouriroosjen.hardcoreSMPPlugin.utils;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for all things regarding dates and times.
 *
 * @author Jouri Roosjen
 * @version 1.0.0
 */
public class DateTimeUtil {
    /**
     * Parse the remaining time input.
     *
     * @param input The user input.
     * @return The duration of the timer.
     */
    public static Duration parseDuration(String input) {
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
     * Format the given instant to a human-readable string.
     *
     * @param instant The instant to format.
     * @return A human-readable string of the instant time.
     */
    public static String formatForDisplay(Instant instant) {
        return instant.atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
