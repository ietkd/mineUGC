package org.mineUGC.items;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.mineUGC.core.model.ItemDefinition;

public class InventoryScanner {
    private final ItemManager itemManager;

    public InventoryScanner(ItemManager itemManager) {
        this.itemManager = itemManager;
    }

    public int replaceInInventory(Player player, ItemDefinition def) {
        int count = 0;
        var inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && def.getId().equals(itemManager.getItemId(item))) {
                inv.setItem(i, itemManager.createItemStack(def));
                count++;
            }
        }
        for (int i = 0; i < player.getInventory().getArmorContents().length; i++) {
            ItemStack item = player.getInventory().getArmorContents()[i];
            if (item != null && def.getId().equals(itemManager.getItemId(item))) {
                player.getInventory().setItem(36 + i, itemManager.createItemStack(def));
                count++;
            }
        }
        return count;
    }
}
