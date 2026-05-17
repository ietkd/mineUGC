package org.mineUGC.items;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;
import org.mineUGC.core.model.ItemDefinition;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ItemManager {
    private final Map<String, ItemDefinition> definitions;
    private final Plugin plugin;
    private final NamespacedKey idKey;

    public ItemManager(Plugin plugin) {
        this.plugin = plugin;
        this.definitions = new ConcurrentHashMap<>();
        this.idKey = new NamespacedKey(plugin, "ugc_id");
    }

    public void index(Collection<ItemDefinition> items) {
        for (ItemDefinition def : items) {
            if (def.getId() != null) {
                definitions.put(def.getId(), def);
            }
        }
    }

    public void index(ItemDefinition def) {
        if (def.getId() != null) {
            definitions.put(def.getId(), def);
        }
    }

    public void remove(String id) {
        definitions.remove(id);
    }

    public ItemDefinition getDefinition(String id) {
        return definitions.get(id);
    }

    public Collection<ItemDefinition> getAllDefinitions() {
        return definitions.values();
    }

    public ItemStack createItemStack(ItemDefinition def) {
        Material mat = Material.getMaterial(def.getMaterial().toUpperCase());
        if (mat == null) mat = Material.STICK;

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            if (def.getName() != null) {
                meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', def.getName()));
            }
            if (def.getLore() != null && !def.getLore().isEmpty()) {
                List<String> colored = def.getLore().stream()
                        .map(l -> org.bukkit.ChatColor.translateAlternateColorCodes('&', l))
                        .toList();
                meta.setLore(colored);
            }
            if (def.getModel() > 0) {
                meta.setCustomModelData(def.getModel());
            }
            if (def.getId() != null) {
                meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, def.getId());
            }
            item.setItemMeta(meta);
        }

        return item;
    }

    public String getItemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
    }

    public boolean isUgcItem(ItemStack item) {
        return getItemId(item) != null;
    }
}
