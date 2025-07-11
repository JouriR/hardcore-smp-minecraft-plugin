package com.jouriroosjen.hardcoreSMPPlugin.managers;

import com.jouriroosjen.hardcoreSMPPlugin.generators.VoidChunkGenerator;
import org.bukkit.*;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Manages word creation
 *
 * @author Jouri Roosjen
 * @version 1.0.0
 */
public class WorldManager {
    private final JavaPlugin plugin;

    private World world;

    /**
     * Constructs a new {@code WorldManager} instance.
     *
     * @param plugin The main plugin instance.
     */
    public WorldManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Creates a new world.
     *
     * @param worldName The name for the new world.
     */
    public void createWorld(String worldName) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            String serializedWorldName = worldName.trim()
                    .toLowerCase()
                    .replace(" ", "_")
                    .replace("-", "_");
            if (serializedWorldName.length() <= 0) return;

            world = Bukkit.getWorld(serializedWorldName);
            if (world != null) return;

            WorldCreator worldCreator = new WorldCreator(serializedWorldName);

            String worldType = plugin.getConfig().getString("worlds." + serializedWorldName + ".type", "NORMAL");
            String worldEnvironment = plugin.getConfig().getString("worlds." + serializedWorldName + ".environment", "NORMAL");
            boolean generateStructures = plugin.getConfig().getBoolean("worlds." + serializedWorldName + ".generate-structures", true);

            try {
                worldCreator.type(WorldType.valueOf(worldType.toUpperCase()));
                worldCreator.environment(World.Environment.valueOf(worldEnvironment.toUpperCase()));
                worldCreator.generateStructures(generateStructures);

                if (serializedWorldName.equals("world_event_rps"))
                    worldCreator.generator(new VoidChunkGenerator());

                world = worldCreator.createWorld();

                if (world == null) {
                    plugin.getLogger().severe("Failed to create world: " + serializedWorldName);
                    return;
                }

                configureWorld(serializedWorldName);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().severe("Invalid world configuration for " + serializedWorldName + ": " + e.getMessage());
            }
        });
    }

    /**
     * Configures the event world settings.
     *
     * @param worldName The name of the world to configure.
     */
    private void configureWorld(String worldName) {
        if (world == null) return;

        String worldDifficulty = plugin.getConfig().getString("worlds." + worldName + ".difficulty", "HARD");
        boolean worldDaylightCycle = plugin.getConfig().getBoolean("worlds." + worldName + ".daylight-cycle", true);
        boolean worldWeatherCycle = plugin.getConfig().getBoolean("worlds." + worldName + ".weather-cycle", true);
        boolean worldMobSpawning = plugin.getConfig().getBoolean("worlds." + worldName + ".mob-spawning", true);
        boolean worldKeepInventory = plugin.getConfig().getBoolean("worlds." + worldName + ".keep-inventory", false);

        world.setDifficulty(Difficulty.valueOf(worldDifficulty.toUpperCase()));

        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, worldDaylightCycle);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, worldWeatherCycle);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, worldMobSpawning);
        world.setGameRule(GameRule.KEEP_INVENTORY, worldKeepInventory);
    }
}
