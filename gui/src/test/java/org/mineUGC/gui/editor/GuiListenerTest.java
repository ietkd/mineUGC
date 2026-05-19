package org.mineUGC.gui.editor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mineUGC.core.model.AbilityConfig;
import org.mineUGC.core.model.ItemDefinition;
import org.mineUGC.core.model.PassiveConfig;

import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GuiListenerTest extends GuiMockTestBase {

    private Player player;
    private ItemDefinition def;

    @BeforeEach
    void setUp() {
        player = server.addPlayer("TestPlayer");
        def = new ItemDefinition();
        def.setId("test_item");
        def.setMaterial("STONE");
        def.setName("&fTest");
    }

    private void startSession() {
        guiListener.startSession(player.getUniqueId(), def, false);
    }

    /**
     * Helper: simulates the full anvil text input flow.
     * 1. Opens anvil input for the given field
     * 2. Sets up a renamed NameTag in the anvil output slot (slot 2)
     * 3. Fires an InventoryClickEvent on slot 2
     */
    private void simulateAnvilInput(String field, String displayName) {
        guiListener.openAnvilInput(player, field);
        InventoryView view = player.getOpenInventory();

        ItemStack renamed = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = renamed.getItemMeta();
        meta.setDisplayName(displayName);
        renamed.setItemMeta(meta);
        view.getTopInventory().setItem(2, renamed);

        InventoryClickEvent event = new InventoryClickEvent(
                view,
                InventoryType.SlotType.RESULT,
                2,
                ClickType.LEFT,
                InventoryAction.PICKUP_ALL
        );
        guiListener.onInventoryClick(event);
    }

    // ==================== Session management ====================

    @Test
    void startSession_shouldCreateSession() {
        assertFalse(guiListener.getEditSession(player.getUniqueId()).isPresent());
        guiListener.startSession(player.getUniqueId(), def, true);
        assertTrue(guiListener.getEditSession(player.getUniqueId()).isPresent());
        assertSame(def, guiListener.getEditSession(player.getUniqueId()).get().getDefinition());
    }

    @Test
    void endSession_shouldRemoveSession() {
        guiListener.startSession(player.getUniqueId(), def, true);
        assertTrue(guiListener.getEditSession(player.getUniqueId()).isPresent());
        guiListener.endSession(player.getUniqueId());
        assertFalse(guiListener.getEditSession(player.getUniqueId()).isPresent());
    }

    // ==================== openAnvilInput ====================

    @Test
    void openAnvilInput_shouldReturnEarlyWhenNoSession() {
        // Should not crash when no session exists
        guiListener.openAnvilInput(player, "name");
        // No assertion needed — just verifying no exception
    }

    @Test
    void openAnvilInput_shouldOpenAnvilInventory() {
        startSession();
        guiListener.openAnvilInput(player, "name");

        InventoryView view = player.getOpenInventory();
        assertNotNull(view);
        Inventory top = view.getTopInventory();
        assertNotNull(top);
        assertEquals(InventoryType.ANVIL, top.getType());
    }

    @Test
    void openAnvilInput_shouldPreFillCurrentValue() {
        startSession();
        guiListener.openAnvilInput(player, "name");

        InventoryView view = player.getOpenInventory();
        ItemStack slot0 = view.getTopInventory().getItem(0);
        assertNotNull(slot0);
        assertEquals(Material.NAME_TAG, slot0.getType());
        assertTrue(slot0.hasItemMeta());
        assertEquals("&fTest", slot0.getItemMeta().getDisplayName());
    }

    @Test
    void openAnvilInput_shouldPreFillPlaceholderWhenNoCurrentValue() {
        startSession();
        // "model" has no current value (model defaults to 0, getCurrentFieldValue returns null when <= 0)
        guiListener.openAnvilInput(player, "model");

        InventoryView view = player.getOpenInventory();
        ItemStack slot0 = view.getTopInventory().getItem(0);
        assertNotNull(slot0);
        assertTrue(slot0.getItemMeta().getDisplayName().startsWith("§7"));
    }

    @Test
    void openAnvilInput_shouldSetPendingField() {
        startSession();
        guiListener.openAnvilInput(player, "name");
        assertTrue(guiListener.getEditSession(player.getUniqueId())
                .map(s -> "name".equals(s.getPendingField())).orElse(false));
    }

    @Test
    void openAnvilInput_shouldHaveAirInSecondSlot() {
        startSession();
        guiListener.openAnvilInput(player, "name");

        InventoryView view = player.getOpenInventory();
        ItemStack slot1 = view.getTopInventory().getItem(1);
        assertNotNull(slot1);
        assertEquals(Material.AIR, slot1.getType());
    }

    // ==================== onInventoryClick guard clauses ====================

    @Test
    void onInventoryClick_shouldIgnoreWhenNotActiveAnvilPlayer() {
        startSession();
        // Create a click event on a regular inventory, not an anvil
        Inventory fakeInv = server.createInventory(null, 9, "test");
        player.openInventory(fakeInv);
        InventoryView view = player.getOpenInventory();

        InventoryClickEvent event = new InventoryClickEvent(
                view,
                InventoryType.SlotType.CONTAINER,
                0,
                ClickType.LEFT,
                InventoryAction.PICKUP_ALL
        );
        // Should not throw
        guiListener.onInventoryClick(event);
    }

    // ==================== Field processing ====================

    @Test
    void anvil_shouldSetName() {
        startSession();
        // Use & for color codes; § codes are stripped by onInventoryClick
        simulateAnvilInput("name", "&6New Name");
        assertEquals("&6New Name", def.getName());
    }

    @Test
    void anvil_shouldSetMaterial() {
        startSession();
        simulateAnvilInput("material", "DIAMOND_BLOCK");
        assertEquals("DIAMOND_BLOCK", def.getMaterial());
    }

    @Test
    void anvil_shouldSetModelWithValidNumber() {
        startSession();
        simulateAnvilInput("model", "10001");
        assertEquals(10001, def.getModel());
    }

    @Test
    void anvil_shouldHandleInvalidModelGracefully() {
        startSession();
        simulateAnvilInput("model", "not_a_number");
        assertEquals(0, def.getModel());
    }

    @Test
    void anvil_shouldSetLoreWithPipeSeparator() {
        startSession();
        simulateAnvilInput("lore", "Line 1|Line 2|Line 3");

        assertNotNull(def.getLore());
        assertEquals(3, def.getLore().size());
        assertEquals("Line 1", def.getLore().get(0));
        assertEquals("Line 3", def.getLore().get(2));
    }

    @Test
    void anvil_shouldSetIdWithSanitization() {
        startSession();
        simulateAnvilInput("id", "My Cool Item@123");
        assertEquals("my_cool_item_123", def.getId());
    }

    @Test
    void anvil_shouldSetSet() {
        startSession();
        simulateAnvilInput("set", "dragon_armor");
        assertEquals("dragon_armor", def.getSet());
    }

    @Test
    void anvil_shouldAddAttribute() {
        startSession();
        simulateAnvilInput("attribute_add", "damage 15.0");

        assertNotNull(def.getAttributes());
        assertEquals(15.0, def.getAttributes().get("damage"), 0.01);
    }

    @Test
    void anvil_shouldRejectInvalidAttributeFormat() {
        startSession();
        simulateAnvilInput("attribute_add", "damage");

        assertNull(def.getAttributes());
    }

    @Test
    void anvil_shouldRejectNonNumericAttributeValue() {
        startSession();
        simulateAnvilInput("attribute_add", "damage abc");

        assertNull(def.getAttributes());
    }

    @Test
    void anvil_shouldAddAbility() {
        startSession();
        simulateAnvilInput("ability_add", "right_click");

        assertNotNull(def.getAbilities());
        assertTrue(def.getAbilities().containsKey("right_click"));
    }

    @Test
    void anvil_shouldRejectDuplicateAbilityKey() {
        startSession();
        simulateAnvilInput("ability_add", "right_click");
        simulateAnvilInput("ability_add", "right_click");

        assertEquals(1, def.getAbilities().size());
    }

    @Test
    void anvil_shouldSetAbilityType() {
        startSession();
        def.setAbilities(new java.util.LinkedHashMap<>());
        def.getAbilities().put("right_click", new AbilityConfig());
        guiListener.getEditSession(player.getUniqueId()).ifPresent(s ->
                s.setEditingAbilityKey("right_click"));

        simulateAnvilInput("ability_type", "lightning_strike");
        assertEquals("lightning_strike", def.getAbilities().get("right_click").getType());
    }

    @Test
    void anvil_shouldSetAbilityCooldown() {
        startSession();
        def.setAbilities(new java.util.LinkedHashMap<>());
        def.getAbilities().put("right_click", new AbilityConfig());
        guiListener.getEditSession(player.getUniqueId()).ifPresent(s ->
                s.setEditingAbilityKey("right_click"));

        simulateAnvilInput("ability_cooldown", "10");
        assertEquals(10, def.getAbilities().get("right_click").getCooldown());
    }

    @Test
    void anvil_shouldSetAbilityMana() {
        startSession();
        def.setAbilities(new java.util.LinkedHashMap<>());
        def.getAbilities().put("right_click", new AbilityConfig());
        guiListener.getEditSession(player.getUniqueId()).ifPresent(s ->
                s.setEditingAbilityKey("right_click"));

        simulateAnvilInput("ability_mana", "25");
        assertEquals(25, def.getAbilities().get("right_click").getManaCost());
    }

    @Test
    void anvil_shouldAddPassive() {
        startSession();
        simulateAnvilInput("passive_add", "on_tick");

        assertNotNull(def.getPassives());
        assertTrue(def.getPassives().containsKey("on_tick"));
    }

    @Test
    void anvil_shouldSetPassiveType() {
        startSession();
        def.setPassives(new java.util.LinkedHashMap<>());
        def.getPassives().put("on_tick", new PassiveConfig());
        guiListener.getEditSession(player.getUniqueId()).ifPresent(s ->
                s.setEditingPassiveKey("on_tick"));

        simulateAnvilInput("passive_type", "potion_effect");
        assertEquals("potion_effect", def.getPassives().get("on_tick").getType());
    }

    @Test
    void anvil_shouldSetPassiveEffect() {
        startSession();
        def.setPassives(new java.util.LinkedHashMap<>());
        def.getPassives().put("on_tick", new PassiveConfig());
        guiListener.getEditSession(player.getUniqueId()).ifPresent(s ->
                s.setEditingPassiveKey("on_tick"));

        simulateAnvilInput("passive_effect", "speed");
        assertEquals("speed", def.getPassives().get("on_tick").getEffect());
    }

    @Test
    void anvil_shouldSetPassiveAmplifier() {
        startSession();
        def.setPassives(new java.util.LinkedHashMap<>());
        def.getPassives().put("on_tick", new PassiveConfig());
        guiListener.getEditSession(player.getUniqueId()).ifPresent(s ->
                s.setEditingPassiveKey("on_tick"));

        simulateAnvilInput("passive_amplifier", "2");
        assertEquals(2, def.getPassives().get("on_tick").getAmplifier());
    }

    @Test
    void anvil_shouldSetRecipeShape() {
        startSession();
        simulateAnvilInput("recipe_shape", "DDD,D D, S");

        assertNotNull(def.getRecipe());
        assertEquals(Arrays.asList("DDD", "D D", " S"), def.getRecipe().getShape());
    }

    @Test
    void anvil_shouldRejectInvalidRecipeShape() {
        startSession();
        simulateAnvilInput("recipe_shape", "DDD,D D");

        assertNull(def.getRecipe());
    }

    @Test
    void anvil_shouldSetRecipeIngredients() {
        startSession();
        simulateAnvilInput("recipe_ingredients", "D=DIAMOND,S=STICK");

        assertNotNull(def.getRecipe());
        Map<String, String> ingredients = def.getRecipe().getIngredients();
        assertEquals("DIAMOND", ingredients.get("D"));
        assertEquals("STICK", ingredients.get("S"));
    }

    // ==================== Cancel flow ====================

    @Test
    void anvil_shouldCancelEditing() {
        startSession();
        guiListener.openAnvilInput(player, "name");
        InventoryView view = player.getOpenInventory();

        ItemStack renamed = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = renamed.getItemMeta();
        meta.setDisplayName("cancel");
        renamed.setItemMeta(meta);
        view.getTopInventory().setItem(2, renamed);

        InventoryClickEvent event = new InventoryClickEvent(
                view,
                InventoryType.SlotType.RESULT,
                2,
                ClickType.LEFT,
                InventoryAction.PICKUP_ALL
        );
        guiListener.onInventoryClick(event);

        // Pending field should be cleared
        assertNull(guiListener.getEditSession(player.getUniqueId())
                .map(s -> s.getPendingField()).orElse(null));
    }

    @Test
    void anvil_shouldCancelEditingCaseInsensitive() {
        startSession();
        guiListener.openAnvilInput(player, "name");
        InventoryView view = player.getOpenInventory();

        ItemStack renamed = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = renamed.getItemMeta();
        meta.setDisplayName("CANCEL");
        renamed.setItemMeta(meta);
        view.getTopInventory().setItem(2, renamed);

        InventoryClickEvent event = new InventoryClickEvent(
                view,
                InventoryType.SlotType.RESULT,
                2,
                ClickType.LEFT,
                InventoryAction.PICKUP_ALL
        );
        guiListener.onInventoryClick(event);

        assertNull(guiListener.getEditSession(player.getUniqueId())
                .map(s -> s.getPendingField()).orElse(null));
    }

    // ==================== onInventoryClose ====================

    @Test
    void onInventoryClose_shouldClearPendingField() {
        startSession();
        guiListener.openAnvilInput(player, "name");

        // Get the anvil view for the close event
        InventoryView view = player.getOpenInventory();
        assertNotNull(view);

        // Verify pending field is set before close
        assertTrue(guiListener.getEditSession(player.getUniqueId())
                .map(s -> "name".equals(s.getPendingField())).orElse(false));

        // Simulate closing the anvil
        InventoryCloseEvent closeEvent = new InventoryCloseEvent(view);
        guiListener.onInventoryClose(closeEvent);

        // Pending field should be cleared
        assertNull(guiListener.getEditSession(player.getUniqueId())
                .map(s -> s.getPendingField()).orElse(null));
    }

    @Test
    void onInventoryClose_shouldNotClearWhenNotActiveAnvilPlayer() {
        startSession();
        // Set pending field without openAnvilInput (simulating a different state)
        guiListener.getEditSession(player.getUniqueId()).ifPresent(s -> s.setPendingField("name"));

        InventoryView view = player.getOpenInventory();
        InventoryCloseEvent closeEvent = new InventoryCloseEvent(view);
        guiListener.onInventoryClose(closeEvent);

        // Pending field should still be set (player wasn't in activeAnvilPlayers)
        assertTrue(guiListener.getEditSession(player.getUniqueId())
                .map(s -> "name".equals(s.getPendingField())).orElse(false));
    }

    // ==================== Accessors ====================

    @Test
    void getMessages_shouldReturnNonNull() {
        assertNotNull(guiListener.getMessages());
    }

    @Test
    void getItemManager_shouldReturnNonNull() {
        assertNotNull(guiListener.getItemManager());
    }

    @Test
    void getItemsDirectory_shouldReturnNonNull() {
        assertNotNull(guiListener.getItemsDirectory());
    }
}
