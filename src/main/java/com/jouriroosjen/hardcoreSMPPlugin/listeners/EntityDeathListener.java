package com.jouriroosjen.hardcoreSMPPlugin.listeners;

import com.jouriroosjen.hardcoreSMPPlugin.enums.PlayerStatisticsEnum;
import com.jouriroosjen.hardcoreSMPPlugin.managers.PlayerStatisticsManager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * Handles entity death events
 *
 * @author Jouri Roosjen
 * @version 1.0.0
 */
public class EntityDeathListener implements Listener {
    private final PlayerStatisticsManager playerStatisticsManager;

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
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();

        if (entity.getKiller() == null || !(entity.getKiller() instanceof Player player)) return;

        switch (event.getEntityType()) {
            case ZOMBIE, SKELETON, CREEPER, SPIDER, CAVE_SPIDER, WITCH, BLAZE, GHAST,
                 MAGMA_CUBE, PHANTOM, SHULKER, SILVERFISH, DROWNED, PILLAGER, VINDICATOR,
                 EVOKER, STRAY, WITHER_SKELETON, GUARDIAN, ELDER_GUARDIAN, HOGLIN, ZOGLIN,
                 PIGLIN_BRUTE, ZOMBIFIED_PIGLIN, ENDERMAN, WARDEN, RAVAGER, BOGGED, BREEZE,
                 CREAKING, ENDER_DRAGON, WITHER, ENDERMITE, GIANT, HUSK, ILLUSIONER, SLIME,
                 VEX, ZOMBIE_VILLAGER:
                playerStatisticsManager.incrementStatistic(player.getUniqueId(), PlayerStatisticsEnum.HOSTILE_MOBS_KILLED, 1);
                break;

            case WOLF, BEE, LLAMA, GOAT, POLAR_BEAR, PANDA, TRADER_LLAMA, DOLPHIN, IRON_GOLEM,
                 PIGLIN, SKELETON_HORSE:
                playerStatisticsManager.incrementStatistic(player.getUniqueId(), PlayerStatisticsEnum.NEUTRAL_MOBS_KILLED, 1);
                break;

            case COW, SHEEP, PIG, CHICKEN, RABBIT, HORSE, MULE, DONKEY, TURTLE,
                 CAT, FOX, STRIDER, PARROT, SQUID, GLOW_SQUID, MOOSHROOM, AXOLOTL,
                 CAMEL, FROG, SNIFFER, ARMADILLO, COD, SALMON, ALLAY, OCELOT, SNOW_GOLEM,
                 VILLAGER, TADPOLE, ZOMBIE_HORSE:
                playerStatisticsManager.incrementStatistic(player.getUniqueId(), PlayerStatisticsEnum.PASSIVE_MOBS_KILLED, 1);
                break;

            case PLAYER:
                playerStatisticsManager.incrementStatistic(player.getUniqueId(), PlayerStatisticsEnum.PLAYERS_KILLED, 1);
                break;

            case BAT:
                playerStatisticsManager.incrementStatistic(player.getUniqueId(), PlayerStatisticsEnum.BATS_KILLED, 1);
                break;

            default:
                break;
        }
    }
}
