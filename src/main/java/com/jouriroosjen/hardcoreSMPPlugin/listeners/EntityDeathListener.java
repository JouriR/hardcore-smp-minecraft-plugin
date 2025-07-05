package com.jouriroosjen.hardcoreSMPPlugin.listeners;

import com.jouriroosjen.hardcoreSMPPlugin.enums.PlayerStatisticsEnum;
import com.jouriroosjen.hardcoreSMPPlugin.managers.PlayerStatisticsManager;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.EnumMap;
import java.util.Set;

/**
 * Handles entity death events
 *
 * @author Jouri Roosjen
 * @version 2.1.0
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

        Player responsiblePlayer = getResponsiblePlayer(entity);
        if (responsiblePlayer == null) return;

        PlayerStatisticsEnum statisticType = MOB_CATEGORIES.get(entity.getType());
        if (statisticType == null) return;

        playerStatisticsManager.incrementStatistic(responsiblePlayer.getUniqueId(), statisticType, 1);
    }

    /**
     * Determines which player is responsible for the entity's death.
     *
     * @param entity The entity that died.
     * @return The responsible player, or null if no player was responsible.
     */
    private Player getResponsiblePlayer(LivingEntity entity) {
        Player directKiller = entity.getKiller();
        if (directKiller != null) return directKiller;

        var lastDamageCause = entity.getLastDamageCause();
        if (lastDamageCause == null) return null;

        // Handle entity damage by entity (projectiles, TNT, etc.)
        if (lastDamageCause instanceof EntityDamageByEntityEvent entityDamageEvent)
            return getPlayerFromDamageSource(entityDamageEvent.getDamager());

        return null;
    }

    /**
     * Determines if damage is done by a player and returns that player.
     *
     * @param damager The entity that caused the damage
     * @return The player responsible for the damage, or null if not found
     */
    private Player getPlayerFromDamageSource(Entity damager) {
        // Direct damage
        if (damager instanceof Player player) return player;

        // Projectile damage
        if (damager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player player) return player;
        }

        // TNT damage
        if (damager instanceof TNTPrimed tnt) {
            Entity source = tnt.getSource();
            if (source instanceof Player player) return player;
        }

        // Area Effect Cloud damage (lingering potions)
        if (damager instanceof AreaEffectCloud cloud) {
            ProjectileSource source = cloud.getSource();
            if (source instanceof Player player) return player;
        }

        // Firework damage
        if (damager instanceof Firework firework) {
            ProjectileSource shooter = firework.getShooter();
            if (shooter instanceof Player player) return player;
        }

        return null;
    }
}
