package org.mineUGC.gui.editor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.mineUGC.core.message.Messages;
import org.mineUGC.core.model.ItemDefinition;
import org.mineUGC.core.model.PassiveConfig;
import org.mineUGC.gui.fastinv.FastInv;
import org.mineUGC.items.ItemManager;

public class PassiveEditInventory extends FastInv {
    public PassiveEditInventory(Player player, ItemDefinition def, String passiveKey,
                                ItemManager itemManager, GuiListener guiListener) {
        super(36, guiListener.getMessages().get("editor.passive-edit-title", passiveKey));

        Messages m = guiListener.getMessages();
        PassiveConfig passive = def.getPassives().get(passiveKey);

        setItem(4, MainMenuInventory.createItem(Material.POTION, "§e" + passiveKey, ""));

        setItem(11, MainMenuInventory.createItem(Material.NAME_TAG, m.get("editor.field-type"),
                        m.get("editor.current", passive.getType() != null ? passive.getType() : "?")),
                e -> {
                    guiListener.promptField(player, "passive_type", m.get("editor.prompt-type"));
                    guiListener.getEditSession(player.getUniqueId()).ifPresent(session ->
                            session.setEditingPassiveKey(passiveKey));
                });
        setItem(13, MainMenuInventory.createItem(Material.POTION, m.get("editor.field-effect"),
                        m.get("editor.current", passive.getEffect() != null ? passive.getEffect() : "?")),
                e -> {
                    guiListener.promptField(player, "passive_effect", m.get("editor.prompt-effect"));
                    guiListener.getEditSession(player.getUniqueId()).ifPresent(session ->
                            session.setEditingPassiveKey(passiveKey));
                });
        setItem(15, MainMenuInventory.createItem(Material.REPEATER, m.get("editor.field-amplifier"),
                        m.get("editor.current", String.valueOf(passive.getAmplifier()))),
                e -> {
                    guiListener.promptField(player, "passive_amplifier", m.get("editor.prompt-amplifier"));
                    guiListener.getEditSession(player.getUniqueId()).ifPresent(session ->
                            session.setEditingPassiveKey(passiveKey));
                });

        setItem(31, MainMenuInventory.createItem(Material.RED_WOOL, m.get("editor.field-delete"),
                        "§7" + m.get("editor.passive-deleted", passiveKey)),
                e -> {
                    def.getPassives().remove(passiveKey);
                    player.sendMessage(m.get("editor.passive-deleted", passiveKey));
                    new PassiveListInventory(player, def, itemManager, guiListener).open(player);
                });
        setItem(35, MainMenuInventory.createItem(Material.BARRIER, m.get("editor.field-back"),
                        m.get("editor.back-lore")),
                e -> new PassiveListInventory(player, def, itemManager, guiListener).open(player));
    }
}
