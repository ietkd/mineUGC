package org.mineUGC.gui.editor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.mineUGC.core.message.Messages;
import org.mineUGC.core.model.AbilityConfig;
import org.mineUGC.core.model.ItemDefinition;
import org.mineUGC.gui.fastinv.FastInv;
import org.mineUGC.items.ItemManager;

public class AbilityEditInventory extends FastInv {
    public AbilityEditInventory(Player player, ItemDefinition def, String abilityKey,
                                ItemManager itemManager, GuiListener guiListener) {
        super(36, guiListener.getMessages().get("editor.ability-edit-title", abilityKey));

        Messages m = guiListener.getMessages();
        AbilityConfig ability = def.getAbilities().get(abilityKey);

        setItem(4, MainMenuInventory.createItem(Material.BLAZE_POWDER, "§e" + abilityKey, ""));

        setItem(11, MainMenuInventory.createItem(Material.NAME_TAG, m.get("editor.field-type"),
                        m.get("editor.current", ability.getType() != null ? ability.getType() : "?")),
                e -> {
                    guiListener.openAnvilInput(player, "ability_type");
                    guiListener.getEditSession(player.getUniqueId()).ifPresent(session ->
                            session.setEditingAbilityKey(abilityKey));
                });
        setItem(13, MainMenuInventory.createItem(Material.CLOCK, m.get("editor.field-cooldown"),
                        m.get("editor.current", ability.getCooldown() + "s")),
                e -> {
                    guiListener.openAnvilInput(player, "ability_cooldown");
                    guiListener.getEditSession(player.getUniqueId()).ifPresent(session ->
                            session.setEditingAbilityKey(abilityKey));
                });
        setItem(15, MainMenuInventory.createItem(Material.EXPERIENCE_BOTTLE, m.get("editor.field-mana"),
                        m.get("editor.current", String.valueOf(ability.getManaCost()))),
                e -> {
                    guiListener.openAnvilInput(player, "ability_mana");
                    guiListener.getEditSession(player.getUniqueId()).ifPresent(session ->
                            session.setEditingAbilityKey(abilityKey));
                });

        setItem(31, MainMenuInventory.createItem(Material.RED_WOOL, m.get("editor.field-delete"),
                        "§7" + m.get("editor.ability-deleted", abilityKey)),
                e -> {
                    def.getAbilities().remove(abilityKey);
                    player.sendMessage(m.get("editor.ability-deleted", abilityKey));
                    new AbilityListInventory(player, def, itemManager, guiListener).open(player);
                });
        setItem(35, MainMenuInventory.createItem(Material.BARRIER, m.get("editor.field-back"),
                        m.get("editor.back-lore")),
                e -> new AbilityListInventory(player, def, itemManager, guiListener).open(player));
    }
}
