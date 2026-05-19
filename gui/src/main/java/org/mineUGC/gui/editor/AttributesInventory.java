package org.mineUGC.gui.editor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.mineUGC.core.message.Messages;
import org.mineUGC.core.model.ItemDefinition;
import org.mineUGC.gui.fastinv.FastInv;
import org.mineUGC.items.ItemManager;

import java.util.Map;

public class AttributesInventory extends FastInv {
    public AttributesInventory(Player player, ItemDefinition def, ItemManager itemManager, GuiListener guiListener) {
        super(54, "§8属性 - " + def.getId());

        Messages m = guiListener.getMessages();
        Map<String, Double> attrs = def.getAttributes();

        int slot = 0;
        if (attrs != null) {
            for (Map.Entry<String, Double> entry : attrs.entrySet()) {
                String attrName = entry.getKey();
                double value = entry.getValue();
                int s = slot;
                setItem(s, MainMenuInventory.createItem(Material.GLOWSTONE_DUST,
                        "§e" + attrName, "§7" + value, "§c点击移除"), click -> {
                    attrs.remove(attrName);
                    player.sendMessage(m.get("editor.attribute-removed", attrName));
                    new AttributesInventory(player, def, itemManager, guiListener).open(player);
                });
                slot++;
            }
        }
        if (slot == 0) {
            setItem(22, MainMenuInventory.createItem(Material.BARRIER,
                    m.get("editor.attr-none"), ""));
        }

        setItem(48, MainMenuInventory.createItem(Material.LIME_WOOL,
                "§a§l" + m.get("editor.click-configure"), "§7" + m.get("editor.prompt-attribute-add")),
                e -> {
                    guiListener.openAnvilInput(player, "attribute_add");
                    guiListener.getEditSession(player.getUniqueId()).ifPresent(session ->
                            session.setReopenAction((p, g) ->
                                    new AttributesInventory(p, def, itemManager, g).open(p)));
                });
        setItem(49, MainMenuInventory.createItem(Material.BARRIER, m.get("editor.field-back"), m.get("editor.back-lore")),
                e -> new ItemEditorInventory(player, def, itemManager, guiListener).open(player));
    }
}
