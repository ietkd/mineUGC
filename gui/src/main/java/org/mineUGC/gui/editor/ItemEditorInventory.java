package org.mineUGC.gui.editor;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.mineUGC.core.model.AbilityConfig;
import org.mineUGC.core.model.ItemDefinition;
import org.mineUGC.core.model.PassiveConfig;
import org.mineUGC.gui.fastinv.FastInv;
import org.mineUGC.items.ItemManager;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class ItemEditorInventory extends FastInv {
    private static final String EDIT_PREFIX = "§8Editing: ";

    public ItemEditorInventory(Player player, ItemDefinition def, ItemManager itemManager, GuiListener guiListener) {
        super(36, EDIT_PREFIX + def.getId());

        ItemStack preview = itemManager.createItemStack(def);
        setItem(4, preview);

        setItem(10, createItem(Material.NAME_TAG, "§eName", current(def.getName())),
                e -> guiListener.promptField(player, "name", "Enter new display name (use & for colors):"));
        setItem(11, createItem(Material.GRASS_BLOCK, "§eMaterial", current(def.getMaterial())),
                e -> guiListener.promptField(player, "material", "Enter material name (e.g. DIAMOND_SWORD):"));
        setItem(12, createItem(Material.ITEM_FRAME, "§eModel ID", current(String.valueOf(def.getModel()))),
                e -> guiListener.promptField(player, "model", "Enter custom model data number:"));
        setItem(13, createItem(Material.BOOK, "§eLore",
                        def.getLore() != null ? def.getLore().size() + " lines" : "None"),
                e -> guiListener.promptField(player, "lore", "Enter lore lines separated by | (pipe):"));
        setItem(14, createItem(Material.DIAMOND_SWORD, "§eAttributes", "§7Click to configure"),
                e -> player.sendMessage("§eAttributes: coming soon — edit in the YAML file directly"));

        setItem(20, createItem(Material.BLAZE_POWDER, "§eAbilities", "§7Click to configure"),
                e -> player.sendMessage("§eAbilities: coming soon — edit in the YAML file directly"));
        setItem(21, createItem(Material.POTION, "§ePassives", "§7Click to configure"),
                e -> player.sendMessage("§ePassives: coming soon — edit in the YAML file directly"));
        setItem(22, createItem(Material.ENDER_EYE, "§eSet Bonus", "§7Click to configure"),
                e -> player.sendMessage("§eSet Bonus: coming soon — edit in the YAML file directly"));
        setItem(23, createItem(Material.CRAFTING_TABLE, "§eRecipe", "§7Click to configure"),
                e -> player.sendMessage("§eRecipe: coming soon — edit in the YAML file directly"));

        setItem(31, createItem(Material.LIME_WOOL, "§a§lSave", "§7Save changes and reload"),
                e -> saveDefinition(player, def, guiListener));
        setItem(35, createItem(Material.BARRIER, "§cBack", "§7Return to main menu"),
                e -> {
                    guiListener.endSession(player.getUniqueId());
                    new MainMenuInventory(player, itemManager, guiListener).open(player);
                });
    }

    private void saveDefinition(Player player, ItemDefinition def, GuiListener guiListener) {
        if (def.getId() == null || def.getId().isEmpty()) {
            player.sendMessage("§cCannot save: item has no ID.");
            return;
        }

        File file = new File(guiListener.getItemsDirectory(), def.getId() + ".yml");

        try {
            YamlConfiguration config = new YamlConfiguration();
            config.set("id", def.getId());
            config.set("name", def.getName());
            config.set("material", def.getMaterial());
            config.set("model", def.getModel() > 0 ? def.getModel() : null);
            if (def.getLore() != null && !def.getLore().isEmpty()) {
                config.set("lore", def.getLore());
            }

            if (def.getAttributes() != null && !def.getAttributes().isEmpty()) {
                for (Map.Entry<String, Double> entry : def.getAttributes().entrySet()) {
                    config.set("attributes." + entry.getKey(), entry.getValue());
                }
            }
            if (def.getAbilities() != null && !def.getAbilities().isEmpty()) {
                for (Map.Entry<String, AbilityConfig> entry : def.getAbilities().entrySet()) {
                    String base = "abilities." + entry.getKey() + ".";
                    config.set(base + "type", entry.getValue().getType());
                    config.set(base + "cooldown", entry.getValue().getCooldown());
                    config.set(base + "mana_cost", entry.getValue().getManaCost());
                    if (entry.getValue().getParams() != null) {
                        for (Map.Entry<String, Object> p : entry.getValue().getParams().entrySet()) {
                            config.set(base + p.getKey(), p.getValue());
                        }
                    }
                }
            }
            if (def.getPassives() != null && !def.getPassives().isEmpty()) {
                for (Map.Entry<String, PassiveConfig> entry : def.getPassives().entrySet()) {
                    String base = "passives." + entry.getKey() + ".";
                    config.set(base + "type", entry.getValue().getType());
                    config.set(base + "effect", entry.getValue().getEffect());
                    config.set(base + "amplifier", entry.getValue().getAmplifier());
                    if (entry.getValue().getParams() != null) {
                        for (Map.Entry<String, Object> p : entry.getValue().getParams().entrySet()) {
                            config.set(base + p.getKey(), p.getValue());
                        }
                    }
                }
            }
            if (def.getSet() != null) {
                config.set("set", def.getSet());
            }
            if (def.getRecipe() != null && def.getRecipe().isValid()) {
                config.set("recipe.shape", def.getRecipe().getShape());
                if (def.getRecipe().getIngredients() != null) {
                    for (Map.Entry<String, String> ing : def.getRecipe().getIngredients().entrySet()) {
                        config.set("recipe.ingredients." + ing.getKey(), ing.getValue());
                    }
                }
            }

            config.save(file);
            guiListener.getItemManager().index(def);
            player.sendMessage("§aSaved §f" + def.getId() + "§a.");
            guiListener.endSession(player.getUniqueId());
            new MainMenuInventory(player, guiListener.getItemManager(), guiListener).open(player);
        } catch (IOException e) {
            player.sendMessage("§cFailed to save: " + e.getMessage());
        }
    }

    static String current(String value) {
        return value != null ? "§7Current: §f" + value : "§7Not set";
    }

    private static ItemStack createItem(Material material, String name, String... lore) {
        return MainMenuInventory.createItem(material, name, lore);
    }
}
