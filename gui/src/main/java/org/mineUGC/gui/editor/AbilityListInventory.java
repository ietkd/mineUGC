package org.mineUGC.gui.editor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.mineUGC.core.message.Messages;
import org.mineUGC.core.model.AbilityConfig;
import org.mineUGC.core.model.ItemDefinition;
import org.mineUGC.gui.fastinv.FastInv;
import org.mineUGC.items.ItemManager;

import java.util.Map;

public class AbilityListInventory extends FastInv {
    public AbilityListInventory(Player player, ItemDefinition def, ItemManager itemManager, GuiListener guiListener) {
        super(54, guiListener.getMessages().get("editor.abilities-title", def.getId()));

        Messages m = guiListener.getMessages();
        Map<String, AbilityConfig> abilities = def.getAbilities();

        int slot = 0;
        if (abilities != null) {
            for (Map.Entry<String, AbilityConfig> entry : abilities.entrySet()) {
                String key = entry.getKey();
                AbilityConfig ability = entry.getValue();
                int s = slot;
                setItem(s, MainMenuInventory.createItem(Material.BLAZE_POWDER,
                        "§e" + key,
                        "§7Type: " + (ability.getType() != null ? ability.getType() : "?"),
                        "§7Cooldown: " + ability.getCooldown() + "s",
                        "§7Mana: " + ability.getManaCost()), click -> {
                    guiListener.getEditSession(player.getUniqueId()).ifPresent(session ->
                            session.setEditingAbilityKey(key));
                    new AbilityEditInventory(player, def, key, itemManager, guiListener).open(player);
                });
                slot++;
            }
        }
        if (slot == 0) {
            setItem(22, MainMenuInventory.createItem(Material.BARRIER,
                    m.get("editor.ability-none"), ""));
        }

        setItem(48, MainMenuInventory.createItem(Material.LIME_WOOL,
                "§a§l" + m.get("editor.click-configure"), "§7" + m.get("editor.prompt-ability-key")),
                e -> {
                    guiListener.openAnvilInput(player, "ability_add");
                    guiListener.getEditSession(player.getUniqueId()).ifPresent(session ->
                            session.setReopenAction((p, g) ->
                                    new AbilityListInventory(p, def, itemManager, g).open(p)));
                });
        setItem(49, MainMenuInventory.createItem(Material.BARRIER, m.get("editor.field-back"), m.get("editor.back-lore")),
                e -> new ItemEditorInventory(player, def, itemManager, guiListener).open(player));
    }
}
