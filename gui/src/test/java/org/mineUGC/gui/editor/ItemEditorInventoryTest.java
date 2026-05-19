package org.mineUGC.gui.editor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mineUGC.core.model.ItemDefinition;

import static org.junit.jupiter.api.Assertions.*;

class ItemEditorInventoryTest extends GuiMockTestBase {

    private Player player;

    @BeforeEach
    void setUp() {
        player = server.addPlayer("TestPlayer");
    }

    @Test
    void constructor_shouldCreateInventoryWithCorrectSize() {
        ItemDefinition def = new ItemDefinition();
        def.setId("test_sword");
        def.setMaterial("DIAMOND_SWORD");

        ItemEditorInventory inv = new ItemEditorInventory(player, def, itemManager, guiListener);
        assertEquals(36, inv.getInventory().getSize());
    }

    @Test
    void constructor_shouldPlacePreviewAtSlot4() {
        ItemDefinition def = new ItemDefinition();
        def.setId("preview_item");
        def.setMaterial("GOLDEN_APPLE");

        ItemEditorInventory inv = new ItemEditorInventory(player, def, itemManager, guiListener);
        ItemStack preview = inv.getInventory().getItem(4);
        assertNotNull(preview);
        assertEquals(Material.GOLDEN_APPLE, preview.getType());
    }

    @Test
    void constructor_shouldPlaceButtonsAtExpectedSlots() {
        ItemDefinition def = new ItemDefinition();
        def.setId("button_test");
        def.setMaterial("STICK");

        ItemEditorInventory inv = new ItemEditorInventory(player, def, itemManager, guiListener);

        assertNotNull(inv.getInventory().getItem(10)); // Name
        assertNotNull(inv.getInventory().getItem(11)); // Material
        assertNotNull(inv.getInventory().getItem(12)); // Model
        assertNotNull(inv.getInventory().getItem(13)); // Lore
        assertNotNull(inv.getInventory().getItem(14)); // ID
        assertNotNull(inv.getInventory().getItem(15)); // Attributes
        assertNotNull(inv.getInventory().getItem(20)); // Abilities
        assertNotNull(inv.getInventory().getItem(21)); // Passives
        assertNotNull(inv.getInventory().getItem(22)); // Set Bonus
        assertNotNull(inv.getInventory().getItem(23)); // Recipe
        assertNotNull(inv.getInventory().getItem(31)); // Save
        assertNotNull(inv.getInventory().getItem(35)); // Back
    }
}
