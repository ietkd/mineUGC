package org.mineUGC.gui.editor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mineUGC.core.model.ItemDefinition;
import org.mineUGC.core.model.PassiveConfig;

import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.*;

class PassiveEditInventoryTest extends GuiMockTestBase {

    private Player player;

    @BeforeEach
    void setUp() {
        player = server.addPlayer("TestPlayer");
    }

    @Test
    void constructor_shouldCreateInventoryWithCorrectSize() {
        ItemDefinition def = defWithPassive("pass_edit", "on_tick");

        PassiveEditInventory inv = new PassiveEditInventory(player, def, "on_tick", itemManager, guiListener);
        assertEquals(36, inv.getInventory().getSize());
    }

    @Test
    void constructor_shouldShowPassivePreviewAtSlot4() {
        ItemDefinition def = defWithPassive("preview_pass", "on_tick");

        PassiveEditInventory inv = new PassiveEditInventory(player, def, "on_tick", itemManager, guiListener);
        ItemStack preview = inv.getInventory().getItem(4);
        assertNotNull(preview);
        assertEquals(Material.POTION, preview.getType());
    }

    @Test
    void constructor_shouldShowEditButtons() {
        ItemDefinition def = def("pass_edit_buttons");
        def.setPassives(new LinkedHashMap<>());
        PassiveConfig passive = new PassiveConfig();
        passive.setType("potion_effect");
        passive.setEffect("speed");
        passive.setAmplifier(2);
        def.getPassives().put("on_tick", passive);

        PassiveEditInventory inv = new PassiveEditInventory(player, def, "on_tick", itemManager, guiListener);

        assertNotNull(inv.getInventory().getItem(11)); // Type
        assertNotNull(inv.getInventory().getItem(13)); // Effect
        assertNotNull(inv.getInventory().getItem(15)); // Amplifier
        assertNotNull(inv.getInventory().getItem(31)); // Delete
        assertNotNull(inv.getInventory().getItem(35)); // Back
    }

    private static ItemDefinition def(String id) {
        ItemDefinition d = new ItemDefinition();
        d.setId(id);
        d.setMaterial("STONE");
        return d;
    }

    private static ItemDefinition defWithPassive(String id, String key) {
        ItemDefinition d = def(id);
        d.setPassives(new LinkedHashMap<>());
        d.getPassives().put(key, new PassiveConfig());
        return d;
    }
}
