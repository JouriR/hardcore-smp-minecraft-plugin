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
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Handles player interact events.
 *
 * @author Jouri Roosjen
 * @version 2.0.0
 */
public class PlayerInteractListener implements Listener {
    private final PlayerStatisticsManager playerStatisticsManager;

    private static final Map<Material, PlayerStatisticsEnum> MATERIAL_TO_STATISTIC = new EnumMap<>(Material.class);

    private static final Set<Material> CHEST_MATERIALS = EnumSet.of(
            Material.CHEST, Material.ENDER_CHEST, Material.CHEST_MINECART,
            Material.ACACIA_CHEST_BOAT, Material.BIRCH_CHEST_BOAT, Material.CHERRY_CHEST_BOAT,
            Material.DARK_OAK_CHEST_BOAT, Material.JUNGLE_CHEST_BOAT, Material.MANGROVE_CHEST_BOAT,
            Material.OAK_CHEST_BOAT, Material.PALE_OAK_CHEST_BOAT, Material.SPRUCE_CHEST_BOAT
    );

    static {
        MATERIAL_TO_STATISTIC.put(Material.CAKE, PlayerStatisticsEnum.CAKE_CONSUMED);
        MATERIAL_TO_STATISTIC.put(Material.TRAPPED_CHEST, PlayerStatisticsEnum.TRAPPED_CHEST_OPENED);
        MATERIAL_TO_STATISTIC.put(Material.BARREL, PlayerStatisticsEnum.BARREL_OPENED);

        // Add all chest materials
        for (Material chest : CHEST_MATERIALS) {
            MATERIAL_TO_STATISTIC.put(chest, PlayerStatisticsEnum.CHEST_OPENED);
        }

        // Add all shulker box materials
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
