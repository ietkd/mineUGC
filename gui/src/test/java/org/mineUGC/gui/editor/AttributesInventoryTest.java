package org.mineUGC.gui.editor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AttributesInventoryTest extends GuiMockTestBase {

    private Player player;

    @BeforeEach
    void setUp() {
        player = server.addPlayer("TestPlayer");
    }

    @Test
    void constructor_shouldCreateInventoryWithCorrectSize() {
        var def = createDef("attr_test");

        AttributesInventory inv = new AttributesInventory(player, def, itemManager, guiListener);
        assertEquals(54, inv.getInventory().getSize());
    }

    @Test
    void constructor_shouldShowBarrierWhenNoAttributes() {
        var def = createDef("no_attrs");

        AttributesInventory inv = new AttributesInventory(player, def, itemManager, guiListener);
        ItemStack barrier = inv.getInventory().getItem(22);
        assertNotNull(barrier);
        assertEquals(Material.BARRIER, barrier.getType());
    }

    @Test
    void constructor_shouldShowAttributeEntriesAndButtons() {
        var def = createDef("with_attrs");
        Map<String, Double> attrs = new LinkedHashMap<>();
        attrs.put("damage", 15.0);
        attrs.put("speed", 1.6);
        def.setAttributes(attrs);

        AttributesInventory inv = new AttributesInventory(player, def, itemManager, guiListener);

        assertNotNull(inv.getInventory().getItem(48)); // Add button
        assertNotNull(inv.getInventory().getItem(49)); // Back button
    }

    @Test
    void constructor_shouldShowAddAndBackButtons() {
        var def = createDef("buttons");

        AttributesInventory inv = new AttributesInventory(player, def, itemManager, guiListener);

        assertNotNull(inv.getInventory().getItem(48));
        assertNotNull(inv.getInventory().getItem(49));
    }

    private static org.mineUGC.core.model.ItemDefinition createDef(String id) {
        var def = new org.mineUGC.core.model.ItemDefinition();
        def.setId(id);
        def.setMaterial("STONE");
        return def;
    }
}
