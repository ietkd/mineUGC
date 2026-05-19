package org.mineUGC.gui.editor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mineUGC.core.model.ItemDefinition;
import org.mineUGC.core.model.RecipeConfig;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RecipeInventoryTest extends GuiMockTestBase {

    private Player player;

    @BeforeEach
    void setUp() {
        player = server.addPlayer("TestPlayer");
    }

    @Test
    void constructor_shouldCreateInventoryWithCorrectSize() {
        ItemDefinition def = def("recipe_test");

        RecipeInventory inv = new RecipeInventory(player, def, itemManager, guiListener);
        assertEquals(54, inv.getInventory().getSize());
    }

    @Test
    void constructor_shouldShowPlaceholderGridWhenNoRecipe() {
        ItemDefinition def = def("no_recipe");

        RecipeInventory inv = new RecipeInventory(player, def, itemManager, guiListener);

        for (int i = 0; i < 9; i++) {
            ItemStack slot = inv.getInventory().getItem(i);
            assertNotNull(slot);
            assertEquals(Material.AIR, slot.getType());
        }
    }

    @Test
    void constructor_shouldShowShapeSlotsWhenRecipeHasShape() {
        ItemDefinition def = def("recipe_shape");
        RecipeConfig recipe = new RecipeConfig();
        recipe.setShape(Arrays.asList("DDD", "D D", " S"));
        Map<String, String> ingredients = new LinkedHashMap<>();
        ingredients.put("D", "DIAMOND");
        ingredients.put("S", "STICK");
        recipe.setIngredients(ingredients);
        def.setRecipe(recipe);

        RecipeInventory inv = new RecipeInventory(player, def, itemManager, guiListener);

        // Slot 0 = 'D' → GRAY_STAINED_GLASS_PANE
        ItemStack slot0 = inv.getInventory().getItem(0);
        assertNotNull(slot0);
        assertEquals(Material.GRAY_STAINED_GLASS_PANE, slot0.getType());

        // Slot 10 = row 1, col 1 → "D D" index 1 → ' ' → AIR
        ItemStack slot10 = inv.getInventory().getItem(10);
        assertNotNull(slot10);
        assertEquals(Material.AIR, slot10.getType());
    }

    @Test
    void constructor_shouldShowEditAndDeleteButtons() {
        ItemDefinition def = def("recipe_buttons");
        def.setRecipe(new RecipeConfig());

        RecipeInventory inv = new RecipeInventory(player, def, itemManager, guiListener);

        assertNotNull(inv.getInventory().getItem(30)); // Edit shape
        assertNotNull(inv.getInventory().getItem(32)); // Edit ingredients
        assertNotNull(inv.getInventory().getItem(34)); // Clear recipe
        assertNotNull(inv.getInventory().getItem(49)); // Back
    }

    private static ItemDefinition def(String id) {
        ItemDefinition d = new ItemDefinition();
        d.setId(id);
        d.setMaterial("STONE");
        return d;
    }
}
