package com.jouriroosjen.hardcoreSMPPlugin.listeners;

import com.jouriroosjen.hardcoreSMPPlugin.enums.PlayerStatisticsEnum;
import com.jouriroosjen.hardcoreSMPPlugin.managers.PlayerStatisticsManager;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.EnumMap;
import java.util.Set;

/**
 * Handles entity death events
 *
 * @author Jouri Roosjen
 * @version 2.0.0
 */
public class EntityDeathListener implements Listener {
    private final PlayerStatisticsManager playerStatisticsManager;

    private static final EnumMap<EntityType, PlayerStatisticsEnum> MOB_CATEGORIES = new EnumMap<>(EntityType.class);

    static {
        // Hostile mobs
        Set<EntityType> hostileMobs = Set.of(
                EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER, EntityType.SPIDER,
                EntityType.CAVE_SPIDER, EntityType.WITCH, EntityType.BLAZE, EntityType.GHAST,
                EntityType.MAGMA_CUBE, EntityType.PHANTOM, EntityType.SHULKER, EntityType.SILVERFISH,
                EntityType.DROWNED, EntityType.PILLAGER, EntityType.VINDICATOR, EntityType.EVOKER,
                EntityType.STRAY, EntityType.WITHER_SKELETON, EntityType.GUARDIAN, EntityType.ELDER_GUARDIAN,
                EntityType.HOGLIN, EntityType.ZOGLIN, EntityType.PIGLIN_BRUTE, EntityType.ZOMBIFIED_PIGLIN,
                EntityType.ENDERMAN, EntityType.WARDEN, EntityType.RAVAGER, EntityType.BOGGED,
                EntityType.BREEZE, EntityType.CREAKING, EntityType.ENDER_DRAGON, EntityType.WITHER,
                EntityType.ENDERMITE, EntityType.GIANT, EntityType.HUSK, EntityType.ILLUSIONER,
                EntityType.SLIME, EntityType.VEX, EntityType.ZOMBIE_VILLAGER
        );

        // Neutral mobs
        Set<EntityType> neutralMobs = Set.of(
                EntityType.WOLF, EntityType.BEE, EntityType.LLAMA, EntityType.GOAT,
                EntityType.POLAR_BEAR, EntityType.PANDA, EntityType.TRADER_LLAMA,
                EntityType.DOLPHIN, EntityType.IRON_GOLEM, EntityType.PIGLIN, EntityType.SKELETON_HORSE
        );

        // Passive mobs
        Set<EntityType> passiveMobs = Set.of(
                EntityType.COW, EntityType.SHEEP, EntityType.PIG, EntityType.CHICKEN,
                EntityType.RABBIT, EntityType.HORSE, EntityType.MULE, EntityType.DONKEY,
                EntityType.TURTLE, EntityType.CAT, EntityType.FOX, EntityType.STRIDER,
                EntityType.PARROT, EntityType.SQUID, EntityType.GLOW_SQUID, EntityType.MOOSHROOM,
                EntityType.AXOLOTL, EntityType.CAMEL, EntityType.FROG, EntityType.SNIFFER,
                EntityType.ARMADILLO, EntityType.COD, EntityType.SALMON, EntityType.ALLAY,
                EntityType.OCELOT, EntityType.SNOW_GOLEM, EntityType.VILLAGER, EntityType.TADPOLE,
                EntityType.ZOMBIE_HORSE
        );

        // Populate the lookup map
        hostileMobs.forEach(type -> MOB_CATEGORIES.put(type, PlayerStatisticsEnum.HOSTILE_MOBS_KILLED));
        neutralMobs.forEach(type -> MOB_CATEGORIES.put(type, PlayerStatisticsEnum.NEUTRAL_MOBS_KILLED));
        passiveMobs.forEach(type -> MOB_CATEGORIES.put(type, PlayerStatisticsEnum.PASSIVE_MOBS_KILLED));

        // Special cases
        MOB_CATEGORIES.put(EntityType.PLAYER, PlayerStatisticsEnum.PLAYERS_KILLED);
        MOB_CATEGORIES.put(EntityType.BAT, PlayerStatisticsEnum.BATS_KILLED);
    }

    /**
     * Constructs a new {@code EntityDeathListener} instance.
     *
     * @param playerStatisticsManager The {@code playerStatisticsManager} instance.
     */
    public EntityDeathListener(PlayerStatisticsManager playerStatisticsManager) {
        this.playerStatisticsManager = playerStatisticsManager;
    }

    /**
     * Event handler for entity death events.
     *
     * @param event The entity death event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();

        if (!(entity.getKiller() instanceof Player player)) return;

        PlayerStatisticsEnum statisticType = MOB_CATEGORIES.get(entity.getType());
        if (statisticType == null) return;

        playerStatisticsManager.incrementStatistic(player.getUniqueId(), statisticType, 1);
    }
}
