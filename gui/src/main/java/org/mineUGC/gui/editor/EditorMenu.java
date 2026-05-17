package org.mineUGC.gui.editor;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.mineUGC.core.model.ItemDefinition;
import org.mineUGC.items.ItemManager;

import java.util.*;

public class EditorMenu {
    private static final String MENU_TITLE = "§8UGC Item Editor";
    private static final int MENU_SIZE = 54;

    private final ItemManager itemManager;

    public EditorMenu(ItemManager itemManager) {
        this.itemManager = itemManager;
    }

    public void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, MENU_SIZE, MENU_TITLE);

        inv.setItem(10, createGuiItem(Material.GREEN_WOOL, "§a§lNew Item", "§7Create a new custom item"));

        int slot = 18;
        for (ItemDefinition def : itemManager.getAllDefinitions()) {
            if (slot >= 45) break;
            ItemStack display = itemManager.createItemStack(def);
            var meta = display.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                lore.add("");
                lore.add("§eLeft-click to edit");
                lore.add("§eRight-click to give to self");
                meta.setLore(lore);
                display.setItemMeta(meta);
            }
            inv.setItem(slot++, display);
        }

        player.openInventory(inv);
    }

    public void openItemEditor(Player player, ItemDefinition def) {
        Inventory inv = Bukkit.createInventory(null, 36, "§8Editing: " + def.getId());

        ItemStack preview = itemManager.createItemStack(def);
        inv.setItem(4, preview);

        inv.setItem(10, createGuiItem(Material.NAME_TAG, "§eName", current(def.getName())));
        inv.setItem(11, createGuiItem(Material.GRASS_BLOCK, "§eMaterial", current(def.getMaterial())));
        inv.setItem(12, createGuiItem(Material.ITEM_FRAME, "§eModel ID", current(String.valueOf(def.getModel()))));
        inv.setItem(13, createGuiItem(Material.BOOK, "§eLore", def.getLore() != null ? def.getLore().size() + " lines" : "None"));
        inv.setItem(14, createGuiItem(Material.DIAMOND_SWORD, "§eAttributes", "§7Click to configure"));

        inv.setItem(20, createGuiItem(Material.BLAZE_POWDER, "§eAbilities", "§7Click to configure"));
        inv.setItem(21, createGuiItem(Material.POTION, "§ePassives", "§7Click to configure"));
        inv.setItem(22, createGuiItem(Material.ENDER_EYE, "§eSet Bonus", "§7Click to configure"));
        inv.setItem(23, createGuiItem(Material.CRAFTING_TABLE, "§eRecipe", "§7Click to configure"));

        inv.setItem(31, createGuiItem(Material.LIME_WOOL, "§a§lSave", "§7Save changes and reload"));
        inv.setItem(35, createGuiItem(Material.BARRIER, "§cBack", "§7Return to main menu"));

        player.openInventory(inv);
    }

    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    private String current(String value) {
        return value != null ? "§7Current: §f" + value : "§7Not set";
    }
}
