package com.jouriroosjen.hardcoreSMPPlugin.listeners;

import com.jouriroosjen.hardcoreSMPPlugin.enums.PlayerStatisticsEnum;
import com.jouriroosjen.hardcoreSMPPlugin.managers.PlayerStatisticsManager;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.EnumMap;
import java.util.Map;

/**
 * Handles player interact events.
 *
 * @author Jouri Roosjen
 * @version 2.0.1
 */
public class PlayerInteractListener implements Listener {
    private final PlayerStatisticsManager playerStatisticsManager;

    private static final Map<Material, PlayerStatisticsEnum> MATERIAL_TO_STATISTIC = new EnumMap<>(Material.class);

    static {
        MATERIAL_TO_STATISTIC.put(Material.CAKE, PlayerStatisticsEnum.CAKE_CONSUMED);

        MATERIAL_TO_STATISTIC.put(Material.CHEST, PlayerStatisticsEnum.CHEST_OPENED);
        MATERIAL_TO_STATISTIC.put(Material.ENDER_CHEST, PlayerStatisticsEnum.CHEST_OPENED);
        MATERIAL_TO_STATISTIC.put(Material.TRAPPED_CHEST, PlayerStatisticsEnum.TRAPPED_CHEST_OPENED);
        MATERIAL_TO_STATISTIC.put(Material.BARREL, PlayerStatisticsEnum.BARREL_OPENED);

        for (Material shulkerBox : Tag.SHULKER_BOXES.getValues()) {
            MATERIAL_TO_STATISTIC.put(shulkerBox, PlayerStatisticsEnum.SHULKER_BOX_OPENED);
        }
    }

    /**
     * Constructs a new {@code PlayerInteractListener} instance.
     *
     * @param playerStatisticsManager The {@code playerStatisticsManager} instance.
     */
    public PlayerInteractListener(PlayerStatisticsManager playerStatisticsManager) {
        this.playerStatisticsManager = playerStatisticsManager;
    }

    /**
     * Event handler for player interact events.
     *
     * @param event The player interact event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;

        Material blockType = event.getClickedBlock().getType();
        PlayerStatisticsEnum statistic = MATERIAL_TO_STATISTIC.get(blockType);

        if (statistic == null) return;

        Player player = event.getPlayer();

        // Special handling for cake consumption, only count if player is actually hungry
        if (statistic == PlayerStatisticsEnum.CAKE_CONSUMED && player.getFoodLevel() >= 20) return;

        playerStatisticsManager.incrementStatistic(player.getUniqueId(), statistic, 1);
    }
}
