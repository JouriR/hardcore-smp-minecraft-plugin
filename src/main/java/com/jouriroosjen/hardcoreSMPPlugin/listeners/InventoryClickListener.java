package com.jouriroosjen.hardcoreSMPPlugin.listeners;

import com.jouriroosjen.hardcoreSMPPlugin.enums.PlayerStatisticsEnum;
import com.jouriroosjen.hardcoreSMPPlugin.managers.PlayerStatisticsManager;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * Handles inventory click events.
 *
 * @author Jouri Roosjen
 * @version 1.0.0
 */
public class InventoryClickListener implements Listener {
    private final PlayerStatisticsManager playerStatisticsManager;

    /**
     * Constructs a new {@code InventoryClickListener} instance.
     *
     * @param playerStatisticsManager The {@code playerStatisticsManager} instance.
     */
    public InventoryClickListener(PlayerStatisticsManager playerStatisticsManager) {
        this.playerStatisticsManager = playerStatisticsManager;
    }

    /**
     * Event handler for anvil enchant events.
     *
     * @param event The inventory click event.
     */
    @EventHandler
    public void onAnvilEnchant(InventoryClickEvent event) {
        if (!(event.getInventory() instanceof AnvilInventory anvil)) return;
        if (event.getSlotType() != InventoryType.SlotType.RESULT) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // Only trigger when they actually take the item
        if (event.getAction() != InventoryAction.PICKUP_ALL &&
                event.getAction() != InventoryAction.MOVE_TO_OTHER_INVENTORY) return;

        ItemStack result = event.getCurrentItem();
        if (result == null || result.getType() == Material.AIR) return;

        ItemStack first = anvil.getFirstItem();
        ItemStack second = anvil.getSecondItem();
        if (first == null || second == null) return;

        Map<Enchantment, Integer> original = first.getEnchantments();
        Map<Enchantment, Integer> resultEnchants = result.getEnchantments();

        // Get what enchantments came from the second item (book or tool)
        Map<Enchantment, Integer> secondEnchants = second.getEnchantments();

        // Detect new or increased enchantments
        int totalNewOrUpgraded = 0;
        
        for (Map.Entry<Enchantment, Integer> resultEntry : resultEnchants.entrySet()) {
            Enchantment enchant = resultEntry.getKey();
            int newLevel = resultEntry.getValue();
            int oldLevel = original.getOrDefault(enchant, 0);

            if (newLevel > oldLevel) totalNewOrUpgraded++;
        }

        if (totalNewOrUpgraded > 0)
            playerStatisticsManager.incrementStatistic(player.getUniqueId(), PlayerStatisticsEnum.ITEMS_ENCHANTED, 1);
    }
}
