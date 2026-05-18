package org.mineUGC.gui.editor;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.mineUGC.core.message.Messages;
import org.mineUGC.core.model.AbilityConfig;
import org.mineUGC.core.model.ItemDefinition;
import org.mineUGC.core.model.PassiveConfig;
import org.mineUGC.gui.fastinv.FastInv;
import org.mineUGC.items.ItemManager;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class ItemEditorInventory extends FastInv {
    public ItemEditorInventory(Player player, ItemDefinition def, ItemManager itemManager, GuiListener guiListener) {
        super(36, guiListener.getMessages().get("editor.editing-prefix") + def.getId());

        Messages m = guiListener.getMessages();

        ItemStack preview = itemManager.createItemStack(def);
        setItem(4, preview);

        setItem(10, createItem(Material.NAME_TAG, m.get("editor.field-name"), current(def.getName(), m)),
                e -> guiListener.promptField(player, "name", m.get("editor.prompt-name")));
        setItem(11, createItem(Material.GRASS_BLOCK, m.get("editor.field-material"), current(def.getMaterial(), m)),
                e -> guiListener.promptField(player, "material", m.get("editor.prompt-material")));
        setItem(12, createItem(Material.ITEM_FRAME, m.get("editor.field-model"), current(String.valueOf(def.getModel()), m)),
                e -> guiListener.promptField(player, "model", m.get("editor.prompt-model")));
        setItem(13, createItem(Material.BOOK, m.get("editor.field-lore"),
                        def.getLore() != null ? def.getLore().size() + " lines" : m.get("editor.not-set")),
                e -> guiListener.promptField(player, "lore", m.get("editor.prompt-lore")));
        setItem(14, createItem(Material.ANVIL, m.get("editor.field-id"), current(def.getId(), m)),
                e -> guiListener.promptField(player, "id", m.get("editor.prompt-id")));
        setItem(15, createItem(Material.DIAMOND_SWORD, m.get("editor.field-attributes"), m.get("editor.click-configure")),
                e -> new AttributesInventory(player, def, itemManager, guiListener).open(player));

        setItem(20, createItem(Material.BLAZE_POWDER, m.get("editor.field-abilities"), m.get("editor.click-configure")),
                e -> new AbilityListInventory(player, def, itemManager, guiListener).open(player));
        setItem(21, createItem(Material.POTION, m.get("editor.field-passives"), m.get("editor.click-configure")),
                e -> new PassiveListInventory(player, def, itemManager, guiListener).open(player));
        setItem(22, createItem(Material.ENDER_EYE, m.get("editor.field-set-bonus"), current(def.getSet(), m)),
                e -> guiListener.promptField(player, "set", m.get("editor.prompt-set")));
        setItem(23, createItem(Material.CRAFTING_TABLE, m.get("editor.field-recipe"), m.get("editor.click-configure")),
                e -> new RecipeInventory(player, def, itemManager, guiListener).open(player));

        setItem(31, createItem(Material.LIME_WOOL, m.get("editor.field-save"), m.get("editor.save-lore")),
                e -> saveDefinition(player, def, guiListener));
        setItem(35, createItem(Material.BARRIER, m.get("editor.field-back"), m.get("editor.back-lore")),
                e -> {
                    guiListener.endSession(player.getUniqueId());
                    new MainMenuInventory(player, itemManager, guiListener).open(player);
                });
    }

    private void saveDefinition(Player player, ItemDefinition def, GuiListener guiListener) {
        Messages m = guiListener.getMessages();

        if (def.getId() == null || def.getId().isEmpty()) {
            player.sendMessage(m.get("editor.save-no-id"));
            return;
        }

        // Delete old file if ID was renamed
        guiListener.getEditSession(player.getUniqueId()).ifPresent(session -> {
            String oldId = session.getOriginalId();
            if (oldId != null && !oldId.equals(def.getId())) {
                File oldFile = new File(guiListener.getItemsDirectory(), oldId + ".yml");
                if (oldFile.exists()) oldFile.delete();
                guiListener.getItemManager().remove(oldId);
            }
        });

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
            player.sendMessage(m.get("editor.save-success", def.getId()));
            guiListener.endSession(player.getUniqueId());
            new MainMenuInventory(player, guiListener.getItemManager(), guiListener).open(player);
        } catch (IOException e) {
            player.sendMessage(m.get("editor.save-failed", e.getMessage()));
        }
    }

    private static String current(String value, Messages messages) {
        return value != null && !value.equals("null") && !value.isEmpty()
                ? messages.get("editor.current", value)
                : messages.get("editor.not-set");
    }

    private static ItemStack createItem(Material material, String name, String... lore) {
        return MainMenuInventory.createItem(material, name, lore);
    }
}
