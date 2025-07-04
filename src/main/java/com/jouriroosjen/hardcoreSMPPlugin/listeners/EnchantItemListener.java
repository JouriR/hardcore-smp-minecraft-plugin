package com.jouriroosjen.hardcoreSMPPlugin.listeners;

import com.jouriroosjen.hardcoreSMPPlugin.enums.PlayerStatisticsEnum;
import com.jouriroosjen.hardcoreSMPPlugin.managers.PlayerStatisticsManager;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Set;

/**
 * Handles enchant item events.
 *
 * @author Jouri Roosjen
 * @version 2.0.0
 */
public class EnchantItemListener implements Listener {
    private final PlayerStatisticsManager playerStatisticsManager;

    private static final Set<InventoryAction> VALID_PICKUP_ACTIONS = Set.of(
            InventoryAction.PICKUP_ALL,
            InventoryAction.MOVE_TO_OTHER_INVENTORY
    );

    /**
     * Constructs a new {@code EnchantItemListener} instance.
     *
     * @param playerStatisticsManager The {@code playerStatisticsManager} instance.
     */
    public EnchantItemListener(PlayerStatisticsManager playerStatisticsManager) {
        this.playerStatisticsManager = playerStatisticsManager;
    }

    /**
     * Event handler for enchant item events (using enchantment table).
     *
     * @param event The enchant item event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnchantItem(EnchantItemEvent event) {
        playerStatisticsManager.incrementStatistic(event.getEnchanter().getUniqueId(), PlayerStatisticsEnum.ITEMS_ENCHANTED, 1);
    }

    /**
     * Event handler for when players enchant using an anvil.
     *
     * @param event The inventory click event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAnvilEnchant(InventoryClickEvent event) {
        if (!(event.getInventory() instanceof AnvilInventory anvil)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (event.getSlotType() != InventoryType.SlotType.RESULT) return;
        if (!VALID_PICKUP_ACTIONS.contains(event.getAction())) return;

        ItemStack result = event.getCurrentItem();
        if (result == null || result.getType() == Material.AIR) return;

        ItemStack first = anvil.getFirstItem();
        ItemStack second = anvil.getSecondItem();
        if (first == null || second == null) return;
        if (first.getType() == Material.AIR || second.getType() == Material.AIR) return;

        if (hasEnchantmentChange(first, result))
            playerStatisticsManager.incrementStatistic(player.getUniqueId(), PlayerStatisticsEnum.ITEMS_ENCHANTED, 1);
    }

    /**
     * Check if the item has an enchantment change.
     *
     * @param original The original item.
     * @param result   The resulting item.
     * @return True if the item has an enchantment change, otherwise false.
     */
    private boolean hasEnchantmentChange(ItemStack original, ItemStack result) {
        int originalSize = original.getEnchantments().size();
        int resultSize = result.getEnchantments().size();

        // If result has more enchantments, definitely changed
        if (resultSize > originalSize) return true;

        // If same size, check for level upgrades
        if (resultSize == originalSize)
            return hasLevelUpgrades(original.getEnchantments(), result.getEnchantments());

        // If there are fewer enchantments in the result, which shouldn't happen in normal anvil operations
        // but could indicate a repair operation - check if any enchantments were added
        return resultSize > 0 && hasNewEnchantments(original.getEnchantments(), result.getEnchantments());
    }

    /**
     * Check if an enchantment has had an upgrade.
     *
     * @param original The original enchantments with level.
     * @param result   The resulting enchantments with level.
     * @return True if an enchantment was upgraded, otherwise false.
     */
    private boolean hasLevelUpgrades(Map<Enchantment, Integer> original, Map<Enchantment, Integer> result) {
        for (Map.Entry<Enchantment, Integer> entry : result.entrySet()) {
            Integer originalLevel = original.get(entry.getKey());
            if (originalLevel == null || entry.getValue() > originalLevel) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if an enchantment was added.
     *
     * @param original The original enchantments with level.
     * @param result   The resulting enchantments with level.
     * @return True if an enchantment was added, otherwise false.
     */
    private boolean hasNewEnchantments(Map<Enchantment, Integer> original, Map<Enchantment, Integer> result) {
        for (Enchantment enchant : result.keySet()) {
            if (!original.containsKey(enchant)) {
                return true;
            }
        }
        return false;
    }
}
