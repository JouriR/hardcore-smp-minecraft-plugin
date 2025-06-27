package com.jouriroosjen.hardcoreSMPPlugin.listeners;

import com.jouriroosjen.hardcoreSMPPlugin.enums.PlayerStatisticsEnum;
import com.jouriroosjen.hardcoreSMPPlugin.managers.PlayerStatisticsManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Handles player interact events.
 *
 * @author Jouri Roosjen
 * @version 1.0.0
 */
public class PlayerInteractListener implements Listener {
    private final PlayerStatisticsManager playerStatisticsManager;

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
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;

        Player player = event.getPlayer();

        switch (event.getClickedBlock().getType()) {
            case CAKE:
                if (player.getFoodLevel() < 20)
                    playerStatisticsManager.incrementStatistic(player.getUniqueId(), PlayerStatisticsEnum.CAKE_CONSUMED, 1);
                break;

            case CHEST, ENDER_CHEST, CHEST_MINECART, ACACIA_CHEST_BOAT, BIRCH_CHEST_BOAT, CHERRY_CHEST_BOAT,
                 DARK_OAK_CHEST_BOAT, JUNGLE_CHEST_BOAT, MANGROVE_CHEST_BOAT, OAK_CHEST_BOAT, PALE_OAK_CHEST_BOAT,
                 SPRUCE_CHEST_BOAT:
                playerStatisticsManager.incrementStatistic(player.getUniqueId(), PlayerStatisticsEnum.CHEST_OPENED, 1);
                break;

            case TRAPPED_CHEST:
                playerStatisticsManager.incrementStatistic(player.getUniqueId(), PlayerStatisticsEnum.TRAPPED_CHEST_OPENED, 1);
                break;

            case SHULKER_BOX, BLACK_SHULKER_BOX, BLUE_SHULKER_BOX, BROWN_SHULKER_BOX, CYAN_SHULKER_BOX,
                 GRAY_SHULKER_BOX, GREEN_SHULKER_BOX, LIGHT_BLUE_SHULKER_BOX, LIGHT_GRAY_SHULKER_BOX, LIME_SHULKER_BOX,
                 MAGENTA_SHULKER_BOX, RED_SHULKER_BOX, PURPLE_SHULKER_BOX, WHITE_SHULKER_BOX, YELLOW_SHULKER_BOX,
                 ORANGE_SHULKER_BOX, PINK_SHULKER_BOX:
                playerStatisticsManager.incrementStatistic(player.getUniqueId(), PlayerStatisticsEnum.SHULKER_BOX_OPENED, 1);
                break;

            case BARREL:
                playerStatisticsManager.incrementStatistic(player.getUniqueId(), PlayerStatisticsEnum.BARREL_OPENED, 1);
                break;

            default:
                break;
        }
    }
}
