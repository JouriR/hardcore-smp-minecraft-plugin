package com.jouriroosjen.hardcoreSMPPlugin.listeners;

import com.jouriroosjen.hardcoreSMPPlugin.enums.PlayerStatisticsEnum;
import com.jouriroosjen.hardcoreSMPPlugin.managers.PlayerStatisticsManager;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Handles player interact entity events.
 *
 * @author Jouri Roosjen
 * @version 1.0.0
 */
public class PlayerInteractEntityListener implements Listener {
    private final PlayerStatisticsManager playerStatisticsManager;

    private static final Map<EntityType, PlayerStatisticsEnum> ENTITY_TO_STATISTIC = new EnumMap<>(EntityType.class);

    private static final Set<EntityType> CHEST_BOAT_TYPES = EnumSet.of(
            EntityType.ACACIA_CHEST_BOAT, EntityType.BIRCH_CHEST_BOAT, EntityType.CHERRY_CHEST_BOAT,
            EntityType.DARK_OAK_CHEST_BOAT, EntityType.JUNGLE_CHEST_BOAT, EntityType.MANGROVE_CHEST_BOAT,
            EntityType.OAK_CHEST_BOAT, EntityType.PALE_OAK_CHEST_BOAT, EntityType.SPRUCE_CHEST_BOAT
    );

    static {
        // Add all chest boat types
        for (EntityType chestBoat : CHEST_BOAT_TYPES) {
            ENTITY_TO_STATISTIC.put(chestBoat, PlayerStatisticsEnum.CHEST_OPENED);
        }

        // Add chest minecart
        ENTITY_TO_STATISTIC.put(EntityType.CHEST_MINECART, PlayerStatisticsEnum.CHEST_OPENED);
    }

    /**
     * Constructs a new {@code PlayerInteractEntityListener} instance.
     *
     * @param playerStatisticsManager The {@code playerStatisticsManager} instance.
     */
    public PlayerInteractEntityListener(PlayerStatisticsManager playerStatisticsManager) {
        this.playerStatisticsManager = playerStatisticsManager;
    }

    /**
     * Event handler for player interact entity events.
     *
     * @param event The player interact entity event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        EntityType entityType = event.getRightClicked().getType();
        PlayerStatisticsEnum statistic = ENTITY_TO_STATISTIC.get(entityType);

        if (statistic == null) return;

        Player player = event.getPlayer();

        if (entityType == EntityType.CHEST_MINECART || player.isSneaking())
            playerStatisticsManager.incrementStatistic(player.getUniqueId(), statistic, 1);
    }

    /**
     * Event handler for inventory open events.
     *
     * @param event The inventory open event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (event.getInventory().getType() != InventoryType.CHEST) return;

        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof Vehicle vehicle)) return;

        // Check if it's a chest boat that the player is riding
        if (CHEST_BOAT_TYPES.contains(vehicle.getType()) && vehicle.getPassengers().contains(player))
            playerStatisticsManager.incrementStatistic(player.getUniqueId(), PlayerStatisticsEnum.CHEST_OPENED, 1);
    }
}