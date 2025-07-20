package com.jouriroosjen.hardcoreSMPPlugin.commands;

import com.jouriroosjen.hardcoreSMPPlugin.managers.CountdownManager;
import com.jouriroosjen.hardcoreSMPPlugin.utils.DateTimeUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;

public class StartCountdownCommand implements CommandExecutor {
    private final CountdownManager countdownManager;

    /**
     * Construct a new {@code StartCountdownCommand} instance.
     *
     * @param countdownManager The active {@code CountdownManager} instance.
     */
    public StartCountdownCommand(CountdownManager countdownManager) {
        this.countdownManager = countdownManager;
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
            Duration duration = DateTimeUtil.parseDuration(String.join(" ", args));
            Instant endTime = Instant.now().plus(duration);

            String endTimeString = endTime.toString();
            countdownManager.createCountdown(endTimeString);

            sender.sendMessage("Countdown started! Ends at: " + DateTimeUtil.formatForDisplay(endTime));
        } catch (IllegalArgumentException e) {
            sender.sendMessage("Invalid duration format! Use: 1h 30m, 2d, etc.");
        }

        return true;
    }
}
