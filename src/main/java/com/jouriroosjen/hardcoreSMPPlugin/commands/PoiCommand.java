package com.jouriroosjen.hardcoreSMPPlugin.commands;

import com.jouriroosjen.hardcoreSMPPlugin.managers.MarkerManager;
import de.bluecolored.bluemap.api.markers.POIMarker;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.concurrent.CompletableFuture;

/**
 * Command executor for the {@code /poi} command, allowing players to create points of interest.
 *
 * @author Jouri Roosjen
 * @version 1.0.0
 */
public class PoiCommand implements CommandExecutor {
    private final MarkerManager markerManager;

    /**
     * Constructs a new {@code PoiCommand} instance.
     *
     * @param markerManager The {@code MarkerManager} instance.
     */
    public PoiCommand(MarkerManager markerManager) {
        this.markerManager = markerManager;
    }

    /**
     * Executes the poi command logic when a player runs it.
     *
     * @param commandSender The source of the command
     * @param command       The command that was executed
     * @param s             The alias used
     * @param args          The command arguments
     *
     * @return {@code true} if the command was handled, {@code false} otherwise
     */
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {
        if (!(commandSender instanceof Player player)) {
            commandSender.sendMessage(Component.text("Only players can execute this command!", NamedTextColor.RED));
            return true;
        }

        if (args.length <= 0) return false;

        String poiName = String.join(" ", args).trim();
        if (poiName.isEmpty()) return false;

        Location poiLocation = player.getLocation();

        POIMarker marker = POIMarker.builder()
                .label(poiName)
                .position(poiLocation.x(), poiLocation.y(), poiLocation.z())
                .maxDistance(1000)
                .build();

        markerManager.addMarker(marker, player.getWorld());

        handleDiscordMessage(marker);

        return true;
    }

    /**
     * Send message to Discord channel on separate thread.
     *
     * @param marker The marker to send into the channel.
     */
    private void handleDiscordMessage(POIMarker marker) {
        CompletableFuture.runAsync(() -> {
            String discordChannelId = DiscordSRV.config().getString("Channels.poi");
            if (discordChannelId == null || discordChannelId.isEmpty()) return;

            TextChannel discordChannel = DiscordSRV.getPlugin().getJda().getTextChannelById(discordChannelId);
            if (discordChannel == null) return;

            EmbedBuilder embed = new EmbedBuilder();
            embed.setThumbnail("https://static.thenounproject.com/png/2980329-200.png");
            embed.setTitle(marker.getLabel());
            embed.setDescription(marker.getPosition().toString());
            embed.setColor(Color.GREEN);

            discordChannel.sendMessageEmbeds(embed.build()).queue();
        });
    }
}
