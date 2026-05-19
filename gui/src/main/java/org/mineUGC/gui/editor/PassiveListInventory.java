package org.mineUGC.gui.editor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.mineUGC.core.message.Messages;
import org.mineUGC.core.model.ItemDefinition;
import org.mineUGC.core.model.PassiveConfig;
import org.mineUGC.gui.fastinv.FastInv;
import org.mineUGC.items.ItemManager;

import java.util.Map;

public class PassiveListInventory extends FastInv {
    public PassiveListInventory(Player player, ItemDefinition def, ItemManager itemManager, GuiListener guiListener) {
        super(54, guiListener.getMessages().get("editor.passives-title", def.getId()));

        Messages m = guiListener.getMessages();
        Map<String, PassiveConfig> passives = def.getPassives();

        int slot = 0;
        if (passives != null) {
            for (Map.Entry<String, PassiveConfig> entry : passives.entrySet()) {
                String key = entry.getKey();
                PassiveConfig passive = entry.getValue();
                int s = slot;
                setItem(s, MainMenuInventory.createItem(Material.POTION,
                        "§e" + key,
                        "§7Type: " + (passive.getType() != null ? passive.getType() : "?"),
                        "§7Effect: " + (passive.getEffect() != null ? passive.getEffect() : "?"),
                        "§7Amplifier: " + passive.getAmplifier()), click -> {
                    guiListener.getEditSession(player.getUniqueId()).ifPresent(session ->
                            session.setEditingPassiveKey(key));
                    new PassiveEditInventory(player, def, key, itemManager, guiListener).open(player);
                });
                slot++;
            }
        }
        if (slot == 0) {
            setItem(22, MainMenuInventory.createItem(Material.BARRIER,
                    m.get("editor.passive-none"), ""));
        }

        setItem(48, MainMenuInventory.createItem(Material.LIME_WOOL,
                "§a§l" + m.get("editor.click-configure"), "§7" + m.get("editor.prompt-passive-key")),
                e -> {
                    guiListener.openAnvilInput(player, "passive_add");
                    guiListener.getEditSession(player.getUniqueId()).ifPresent(session ->
                            session.setReopenAction((p, g) ->
                                    new PassiveListInventory(p, def, itemManager, g).open(p)));
                });
        setItem(49, MainMenuInventory.createItem(Material.BARRIER, m.get("editor.field-back"), m.get("editor.back-lore")),
                e -> new ItemEditorInventory(player, def, itemManager, guiListener).open(player));
    }
}
