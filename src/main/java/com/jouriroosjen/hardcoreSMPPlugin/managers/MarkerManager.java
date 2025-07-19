package com.jouriroosjen.hardcoreSMPPlugin.managers;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.gson.MarkerGson;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.POIMarker;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

/**
 * Manages the markers set by this plugin on BlueMap.
 *
 * @author Jouri Roosjen
 * @version 1.0.0
 */
public class MarkerManager {
    private final JavaPlugin plugin;
    private final BlueMapAPI blueMapAPI;

    private enum WorldType {
        OVERWORLD("overworld-marker-set.json", "world"),
        NETHER("nether-marker-set.json", "world_nether"),
        END("end-marker-set.json", "world_the_end");

        private final String fileName;
        private final String worldName;

        WorldType(String fileName, String worldName) {
            this.fileName = fileName;
            this.worldName = worldName;
        }
    }

    private final Map<WorldType, File> markerFiles = new EnumMap<>(WorldType.class);
    private final Map<WorldType, MarkerSet> markerSets = new EnumMap<>(WorldType.class);

    /**
     * Constructs a new {@code MarkerManager} instance.
     *
     * @param plugin     The main plugin instance.
     * @param blueMapAPI The BlueMap API instance.
     */
    public MarkerManager(JavaPlugin plugin, BlueMapAPI blueMapAPI) {
        this.plugin = plugin;
        this.blueMapAPI = blueMapAPI;

        for (WorldType worldType : WorldType.values()) {
            markerFiles.put(worldType, new File(plugin.getDataFolder(), worldType.fileName));
        }

        initializeMarkers();
    }

    /**
     * Add a new marker to BlueMap.
     *
     * @param marker The marker instance to add.
     * @param world  The world the marker is made in.
     */
    public void addMarker(POIMarker marker, World world) {
        if (marker == null || world == null) return;

        WorldType worldType = getWorldType(world);
        if (worldType == null) return;

        MarkerSet markerSet = markerSets.get(worldType);
        markerSet.put(marker.getLabel(), marker);

        updateMapMarkers(worldType, markerSet);
        saveMarkerSet(worldType, markerSet);
    }

    /**
     * Gets the WorldType.
     *
     * @param world The world the sender is in.
     *
     * @return The corresponding {@code WorldType}.
     */
    private WorldType getWorldType(World world) {
        String worldName = world.getName();

        for (WorldType worldType : WorldType.values()) {
            if (worldType.worldName.equals(worldName)) {
                return worldType;
            }
        }

        return null;
    }

    /**
     * Initializes markers on BlueMap.
     */
    private void initializeMarkers() {
        for (WorldType worldType : WorldType.values()) {
            try {
                MarkerSet markerSet = loadOrCreateMarkerSet(worldType);
                markerSets.put(worldType, markerSet);
                updateMapMarkers(worldType, markerSet);

                plugin.getLogger().info("Loaded " + worldType.name().toLowerCase() + " markers.");
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to initialize " + worldType.name().toLowerCase() +
                        " markers: " + e.getMessage());
            }
        }
    }

    /**
     * Loads a marker set from file, or creates a new one if it doesn't exist.
     *
     * @param worldType The world type which file should be fetched.
     *
     * @return The loaded, or created, {@code MarkerSet}.
     * @throws IOException If an error occurred while reading the file.
     */
    private MarkerSet loadOrCreateMarkerSet(WorldType worldType) throws IOException {
        File file = markerFiles.get(worldType);

        if (!file.exists())
            return createDefaultMarkerSet(worldType);

        try (FileReader reader = new FileReader(file)) {
            return MarkerGson.INSTANCE.fromJson(reader, MarkerSet.class);
        }
    }

    /**
     * Creates a default marker set and saves it to file.
     *
     * @param worldType The world type to create a default marker set for.
     *
     * @return The created {@code MarkerSet}.
     */
    private MarkerSet createDefaultMarkerSet(WorldType worldType) {
        MarkerSet markerSet = MarkerSet.builder()
                .label("Points of Interest")
                .build();

        saveMarkerSet(worldType, markerSet);
        plugin.getLogger().info("Created " + worldType.name().toLowerCase() + " marker set");

        return markerSet;
    }

    /**
     * Saves a marker set to its file.
     *
     * @param worldType The world type of the marker set.
     * @param markerSet The marker set to save.
     */
    private void saveMarkerSet(WorldType worldType, MarkerSet markerSet) {
        File file = markerFiles.get(worldType);

        try (FileWriter writer = new FileWriter(file)) {
            MarkerGson.INSTANCE.toJson(markerSet, writer);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save " + worldType.name().toLowerCase() +
                    " marker set: " + e.getMessage());
        }
    }

    /**
     * Updates the BlueMap markers.
     *
     * @param worldType The world type to update.
     * @param markerSet The new marker set.
     */
    private void updateMapMarkers(WorldType worldType, MarkerSet markerSet) {
        plugin.getLogger().info("Updating markers for world type: " + worldType.name());

        for (BlueMapMap map : blueMapAPI.getMaps()) {
            if (map.getId().equals(worldType.worldName))
                map.getMarkerSets().put("Points of Interest", markerSet);
        }
    }

    /**
     * Saves all marker sets to their files.
     */
    public void saveAllMarkerSets() {
        for (WorldType worldType : WorldType.values()) {
            MarkerSet markerSet = markerSets.get(worldType);
            if (markerSet == null) return;

            saveMarkerSet(worldType, markerSet);
        }
    }
}
