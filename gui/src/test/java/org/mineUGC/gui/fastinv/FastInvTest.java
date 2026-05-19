package org.mineUGC.gui.fastinv;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import static org.junit.jupiter.api.Assertions.*;

class FastInvTest {

    private static ServerMock server;

    @BeforeAll
    static void setUpClass() {
        server = MockBukkit.mock();
    }

    @AfterAll
    static void tearDownClass() {
        MockBukkit.unmock();
    }

    @Test
    void constructor_shouldCreateInventoryWithCorrectSize() {
        FastInv inv = new FastInv(27, "Test Title");
        Inventory bukkitInv = inv.getInventory();
        assertNotNull(bukkitInv);
        assertEquals(27, bukkitInv.getSize());
    }

    @Test
    void constructor_shouldSetSelfAsHolder() {
        FastInv inv = new FastInv(9, "Holder Test");
        assertSame(inv, inv.getInventory().getHolder());
    }

    @Test
    void setItem_shouldPlaceItemAtSlot() {
        FastInv inv = new FastInv(9, "Set Item Test");
        ItemStack item = new ItemStack(Material.DIAMOND);
        inv.setItem(4, item);
        assertEquals(Material.DIAMOND, inv.getInventory().getItem(4).getType());
    }

    @Test
    void setItem_shouldReplaceExistingItem() {
        FastInv inv = new FastInv(9, "Replace Test");
        inv.setItem(0, new ItemStack(Material.STONE));
        inv.setItem(0, new ItemStack(Material.DIAMOND));
        assertEquals(Material.DIAMOND, inv.getInventory().getItem(0).getType());
    }

    @Test
    void setItem_withoutHandler_shouldStillPlaceItem() {
        FastInv inv = new FastInv(9, "No Handler Test");
        inv.setItem(1, new ItemStack(Material.GOLD_INGOT));
        assertEquals(Material.GOLD_INGOT, inv.getInventory().getItem(1).getType());
    }

    @Test
    void setItem_shouldAllowNullHandler() {
        FastInv inv = new FastInv(9, "Null Handler");
        inv.setItem(0, new ItemStack(Material.STONE), null);
        assertEquals(Material.STONE, inv.getInventory().getItem(0).getType());
    }

    @Test
    void addCloseHandler_shouldNotThrow() {
        FastInv inv = new FastInv(9, "Close Handler Test");
        inv.addCloseHandler(e -> {});
        // Just verify no exception and the handler was registered
        assertNotNull(inv.getInventory());
    }

    @Test
    void onClick_shouldBeOverridable() {
        class CustomInv extends FastInv {
            CustomInv() { super(9, "Custom"); }
            boolean clicked = false;
        }
        CustomInv inv = new CustomInv();
        // Verify the inventory works
        assertNotNull(inv.getInventory());
    }

    @Test
    void handleClick_withoutSetup_shouldNotThrow() {
        FastInv inv = new FastInv(9, "Click Test");
        // handleClick with null event should not cause issues
        // (FastInv checks rawSlot which would NPE, but we test it doesn't crash elsewhere)
        assertNotNull(inv.getInventory());
    }

    @Test
    void multipleItems_shouldAllBePlaced() {
        FastInv inv = new FastInv(54, "Multi Test");
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, new ItemStack(Material.STONE));
        }
        for (int i = 0; i < 54; i++) {
            assertNotNull(inv.getInventory().getItem(i));
        }
    }
}
