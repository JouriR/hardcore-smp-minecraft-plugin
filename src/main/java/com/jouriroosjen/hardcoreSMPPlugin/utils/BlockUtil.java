package com.jouriroosjen.hardcoreSMPPlugin.utils;

import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Utility class for all things regarding blocks.
 *
 * @author Jouri Roosjen
 * @version 1.0.0
 */
public class BlockUtil {
    private static final Set<Material> INDESTRUCTIBLE_MATERIALS = EnumSet.of(
            Material.BEDROCK, Material.OBSIDIAN, Material.FIRE, Material.SOUL_FIRE,
            Material.AIR, Material.VOID_AIR, Material.CAVE_AIR, Material.LIGHT,
            Material.CRYING_OBSIDIAN, Material.BARRIER, Material.COMMAND_BLOCK, Material.CHAIN_COMMAND_BLOCK,
            Material.REPEATING_COMMAND_BLOCK, Material.WATER, Material.LAVA, Material.BUBBLE_COLUMN
    );

    /**
     * Count blocks that can be exploded.
     *
     * @param blocks The list of blocks to check.
     * @return The count of explodable blocks.
     */
    public static long countExplodableBlocks(List<Block> blocks) {
        long count = 0;
        for (Block block : blocks) {
            Material type = block.getType();
            if (type.isSolid() && !type.isAir() && !INDESTRUCTIBLE_MATERIALS.contains(type)) count++;
        }
        return count;
    }
}
