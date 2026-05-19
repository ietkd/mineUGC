package org.mineUGC.gui.editor;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class MainMenuInventoryTest {

    private static ServerMock server;

    @BeforeAll
    static void setUp() {
        server = MockBukkit.mock();
    }

    @AfterAll
    static void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void createItem_shouldSetMaterial() {
        ItemStack item = MainMenuInventory.createItem(Material.DIAMOND, "§eTest", "Lore line");
        assertEquals(Material.DIAMOND, item.getType());
    }

    @Test
    void createItem_shouldSetDisplayName() {
        ItemStack item = MainMenuInventory.createItem(Material.STONE, "§aDisplay", "Lore");
        ItemMeta meta = item.getItemMeta();
        assertNotNull(meta);
        assertEquals("§aDisplay", meta.getDisplayName());
    }

    @Test
    void createItem_shouldSetLore() {
        ItemStack item = MainMenuInventory.createItem(Material.BOOK, "Test", "Line 1", "Line 2", "Line 3");
        ItemMeta meta = item.getItemMeta();
        assertNotNull(meta);
        assertNotNull(meta.getLore());
        assertEquals(3, meta.getLore().size());
        assertEquals(Arrays.asList("Line 1", "Line 2", "Line 3"), meta.getLore());
    }

    @Test
    void createItem_shouldHandleSingleLore() {
        ItemStack item = MainMenuInventory.createItem(Material.GREEN_WOOL, "Button", "Only line");
        ItemMeta meta = item.getItemMeta();
        assertNotNull(meta);
        assertEquals(1, meta.getLore().size());
        assertEquals("Only line", meta.getLore().get(0));
    }

    @Test
    void createItem_shouldHandleNoLore() {
        ItemStack item = MainMenuInventory.createItem(Material.BARRIER, "Back");
        ItemMeta meta = item.getItemMeta();
        assertNotNull(meta);
        // Lore may be null or empty when not provided — both are acceptable
        assertTrue(meta.getLore() == null || meta.getLore().isEmpty());
    }

    @Test
    void createItem_shouldHandleDifferentMaterials() {
        for (Material mat : Arrays.asList(Material.STONE, Material.DIAMOND_SWORD, Material.POTION, Material.AIR)) {
            ItemStack item = MainMenuInventory.createItem(mat, "Test");
            assertEquals(mat, item.getType());
        }
    }
}
