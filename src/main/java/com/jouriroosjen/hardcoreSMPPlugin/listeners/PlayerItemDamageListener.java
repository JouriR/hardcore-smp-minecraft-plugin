package com.jouriroosjen.hardcoreSMPPlugin.listeners;

import com.jouriroosjen.hardcoreSMPPlugin.enums.PlayerStatisticsEnum;
import com.jouriroosjen.hardcoreSMPPlugin.managers.PlayerStatisticsManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Handles player item damage events.
 *
 * @author Jouri Roosjen
 * @version 1.1.0
 */
public class PlayerItemDamageListener implements Listener {
    private final PlayerStatisticsManager playerStatisticsManager;

    /**
     * Constructs a new {@code PlayerItemDamageListener} instance.
     *
     * @param playerStatisticsManager The {@code playerStatisticsManager} instance.
     */
    public PlayerItemDamageListener(PlayerStatisticsManager playerStatisticsManager) {
        this.playerStatisticsManager = playerStatisticsManager;
    }

    /**
     * Event handler for player item damage event.
     *
     * @param event The player item damage event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerItemDamage(PlayerItemDamageEvent event) {
        int damageAmount = event.getDamage();
        if (damageAmount <= 0) return;

        ItemStack item = event.getItem();
        if (!hasDurability(item)) return;

        UUID playerUuid = event.getPlayer().getUniqueId();
        playerStatisticsManager.incrementStatistic(playerUuid, PlayerStatisticsEnum.TOTAL_ITEM_DAMAGE, damageAmount);
    }

    /**
     * Checks if an item has durability and can be damaged.
     *
     * @param item The item to check.
     * @return True if the item has durability, otherwise false.
     */
    private boolean hasDurability(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        return item.getType().getMaxDurability() > 0;
    }
}
