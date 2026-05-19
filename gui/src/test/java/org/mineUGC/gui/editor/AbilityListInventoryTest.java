package org.mineUGC.gui.editor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mineUGC.core.model.AbilityConfig;
import org.mineUGC.core.model.ItemDefinition;

import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.*;

class AbilityListInventoryTest extends GuiMockTestBase {

    private Player player;

    @BeforeEach
    void setUp() {
        player = server.addPlayer("TestPlayer");
    }

    @Test
    void constructor_shouldCreateInventoryWithCorrectSize() {
        ItemDefinition def = def("abil_list");

        AbilityListInventory inv = new AbilityListInventory(player, def, itemManager, guiListener);
        assertEquals(54, inv.getInventory().getSize());
    }

    @Test
    void constructor_shouldShowBarrierWhenNoAbilities() {
        ItemDefinition def = def("no_abil");

        AbilityListInventory inv = new AbilityListInventory(player, def, itemManager, guiListener);
        ItemStack barrier = inv.getInventory().getItem(22);
        assertNotNull(barrier);
        assertEquals(Material.BARRIER, barrier.getType());
    }

    @Test
    void constructor_shouldShowAbilityEntriesWhenPresent() {
        ItemDefinition def = def("with_abil");
        def.setAbilities(new LinkedHashMap<>());
        AbilityConfig ability = new AbilityConfig();
        ability.setType("lightning_strike");
        ability.setCooldown(10);
        def.getAbilities().put("right_click", ability);

        AbilityListInventory inv = new AbilityListInventory(player, def, itemManager, guiListener);

        assertNotNull(inv.getInventory().getItem(0));
    }

    @Test
    void constructor_shouldShowAddAndBackButtons() {
        ItemDefinition def = def("abil_buttons");

        AbilityListInventory inv = new AbilityListInventory(player, def, itemManager, guiListener);

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
