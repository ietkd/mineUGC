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

class PassiveListInventoryTest extends GuiMockTestBase {

    private Player player;

    @BeforeEach
    void setUp() {
        player = server.addPlayer("TestPlayer");
    }

    @Test
    void constructor_shouldCreateInventoryWithCorrectSize() {
        ItemDefinition def = def("pass_list");

        PassiveListInventory inv = new PassiveListInventory(player, def, itemManager, guiListener);
        assertEquals(54, inv.getInventory().getSize());
    }

    @Test
    void constructor_shouldShowBarrierWhenNoPassives() {
        ItemDefinition def = def("no_pass");

        PassiveListInventory inv = new PassiveListInventory(player, def, itemManager, guiListener);
        ItemStack barrier = inv.getInventory().getItem(22);
        assertNotNull(barrier);
        assertEquals(Material.BARRIER, barrier.getType());
    }

    @Test
    void constructor_shouldShowPassiveEntriesWhenPresent() {
        ItemDefinition def = def("with_pass");
        def.setPassives(new LinkedHashMap<>());
        PassiveConfig passive = new PassiveConfig();
        passive.setType("potion_effect");
        passive.setEffect("regeneration");
        passive.setAmplifier(1);
        def.getPassives().put("on_tick", passive);

        PassiveListInventory inv = new PassiveListInventory(player, def, itemManager, guiListener);

        assertNotNull(inv.getInventory().getItem(0));
    }

    @Test
    void constructor_shouldShowAddAndBackButtons() {
        ItemDefinition def = def("pass_buttons");

        PassiveListInventory inv = new PassiveListInventory(player, def, itemManager, guiListener);

        assertNotNull(inv.getInventory().getItem(48));
        assertNotNull(inv.getInventory().getItem(49));
    }

    private static ItemDefinition def(String id) {
        ItemDefinition d = new ItemDefinition();
        d.setId(id);
        d.setMaterial("STONE");
        return d;
    }
}
