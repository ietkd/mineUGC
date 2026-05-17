package org.mineUGC.gui.editor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.mineUGC.core.model.ItemDefinition;
import org.mineUGC.gui.fastinv.FastInv;
import org.mineUGC.items.ItemManager;
import org.mineUGC.items.attributes.AttributeApplier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainMenuInventory extends FastInv {
    private static final String TITLE = "§8UGC Item Editor";

    public MainMenuInventory(Player player, ItemManager itemManager, GuiListener guiListener) {
        super(54, TITLE);

        setItem(10, createItem(Material.GREEN_WOOL, "§a§lNew Item", "§7Create a new custom item"),
                e -> {
                    ItemDefinition def = new ItemDefinition();
                    def.setId("custom_item_" + System.currentTimeMillis());
                    def.setMaterial("STONE");
                    def.setName("&fCustom Item");
                    guiListener.startSession(player.getUniqueId(), def, false);
                    new ItemEditorInventory(player, def, itemManager, guiListener).open(player);
                });

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

            int s = slot;
            setItem(s, display, click -> {
                if (click.isLeftClick()) {
                    guiListener.startSession(player.getUniqueId(), def, true);
                    new ItemEditorInventory(player, def, itemManager, guiListener).open(player);
                } else if (click.isRightClick()) {
                    ItemStack item = itemManager.createItemStack(def);
                    new AttributeApplier().apply(item, def.getAttributes());
                    player.getInventory().addItem(item);
                    player.sendMessage("§aReceived " + (def.getName() != null ? def.getName() : def.getId()) + "§a.");
                }
            });
            slot++;
        }
    }

    static ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }
}
