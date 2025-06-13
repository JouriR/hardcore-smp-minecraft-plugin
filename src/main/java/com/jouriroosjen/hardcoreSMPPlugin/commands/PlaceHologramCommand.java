package com.jouriroosjen.hardcoreSMPPlugin.commands;

import com.jouriroosjen.hardcoreSMPPlugin.enums.HologramEnum;
import com.jouriroosjen.hardcoreSMPPlugin.managers.HologramManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Command executor for the {@code /place-hologram} command.
 *
 * @author Jouri Roosjen
 * @version 1.0.0
 */
public class PlaceHologramCommand implements CommandExecutor {
    private final HologramManager hologramManager;

    /**
     * Constructs a new {@code PlaceHologramCommand} instance.
     *
     * @param hologramManager The hologramManager instance
     */
    public PlaceHologramCommand(HologramManager hologramManager) {
        this.hologramManager = hologramManager;
    }

    /**
     * Executes the place-hologram command logic when a player runs it.
     *
     * @param sender  The source of the command
     * @param command The command that was executed
     * @param label   The alias used
     * @param args    The command arguments
     * @return {@code true} if the command was handled, {@code false} otherwise
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can execute this command!", NamedTextColor.RED));
            return true;
        }

        if (!player.isOp()) {
            player.sendMessage(Component.text("Only OP's are allowed to run this command!", NamedTextColor.RED));
            return true;
        }

        if (args.length != 1 || !existsInEnum(args[0])) return false;

        if (hologramManager.containsHologram(args[0])) {
            player.sendMessage(Component.text("This hologram already exists!", NamedTextColor.RED, TextDecoration.BOLD));
            return true;
        }

        String hologramName = args[0];
        Location hologramLocation = player.getLocation().add(0, 1.75, 0);
        hologramManager.createHologram(hologramName, hologramLocation, HologramEnum.valueOf(hologramName), player);

        return true;
    }

    /**
     * Checks whether the value exist in the enum or not.
     *
     * @param value The value to check.
     * @return {@code true} if the value exists, {@code false} otherwise.
     */
    private boolean existsInEnum(String value) {
        boolean exists = false;
        for (HologramEnum hologramEnum : HologramEnum.values()) {
            if (hologramEnum.name().equalsIgnoreCase(value)) {
                exists = true;
                break;
            }
        }
        return exists;
    }
}
