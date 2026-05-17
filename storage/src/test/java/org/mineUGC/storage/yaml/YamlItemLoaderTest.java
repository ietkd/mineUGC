package org.mineUGC.storage.yaml;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mineUGC.core.model.AbilityConfig;
import org.mineUGC.core.model.ItemDefinition;
import org.mineUGC.core.model.PassiveConfig;
import org.mineUGC.core.model.RecipeConfig;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class YamlItemLoaderTest {

    private final YamlItemLoader loader = new YamlItemLoader();

    @Test
    void testBasicFields(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("test_item.yml").toFile();
        try (FileWriter w = new FileWriter(file)) {
            w.write("id: test_sword\n");
            w.write("name: Test Sword\n");
            w.write("material: DIAMOND_SWORD\n");
            w.write("model: 1\n");
        }

        ItemDefinition def = loader.load(file);
        assertEquals("test_sword", def.getId());
        assertEquals("Test Sword", def.getName());
        assertEquals("DIAMOND_SWORD", def.getMaterial());
        assertEquals(1, def.getModel());
    }

    @Test
    void testLoreParsing(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("lore_item.yml").toFile();
        try (FileWriter w = new FileWriter(file)) {
            w.write("id: lore_item\n");
            w.write("name: Lore Item\n");
            w.write("material: STONE\n");
            w.write("lore:\n");
            w.write("  - 'Line one'\n");
            w.write("  - 'Line two'\n");
        }

        ItemDefinition def = loader.load(file);
        assertEquals(List.of("Line one", "Line two"), def.getLore());
    }

    @Test
    void testAttributesParsing(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("attr_item.yml").toFile();
        try (FileWriter w = new FileWriter(file)) {
            w.write("id: attr_item\n");
            w.write("name: Attr Item\n");
            w.write("material: DIAMOND\n");
            w.write("attributes:\n");
            w.write("  damage: 10.5\n");
            w.write("  speed: 2.0\n");
        }

        ItemDefinition def = loader.load(file);
        Map<String, Double> attrs = def.getAttributes();
        assertNotNull(attrs);
        assertEquals(10.5, attrs.get("damage"), 0.001);
        assertEquals(2.0, attrs.get("speed"), 0.001);
    }

    @Test
    void testAbilityParsing(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("abil_item.yml").toFile();
        try (FileWriter w = new FileWriter(file)) {
            w.write("id: abil_item\n");
            w.write("name: Ability Item\n");
            w.write("material: BLAZE_ROD\n");
            w.write("abilities:\n");
            w.write("  fireball:\n");
            w.write("    type: PROJECTILE\n");
            w.write("    cooldown: 10\n");
            w.write("    mana_cost: 5\n");
            w.write("    damage: 8.0\n");
        }

        ItemDefinition def = loader.load(file);
        Map<String, AbilityConfig> abilities = def.getAbilities();
        assertNotNull(abilities);
        AbilityConfig abil = abilities.get("fireball");
        assertNotNull(abil);
        assertEquals("PROJECTILE", abil.getType());
        assertEquals(10, abil.getCooldown());
        assertEquals(5, abil.getManaCost());
        assertEquals(8.0, abil.getParams().get("damage"));
    }

    @Test
    void testPassiveParsing(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("pass_item.yml").toFile();
        try (FileWriter w = new FileWriter(file)) {
            w.write("id: pass_item\n");
            w.write("name: Passive Item\n");
            w.write("material: LEATHER_BOOTS\n");
            w.write("passives:\n");
            w.write("  speed_boost:\n");
            w.write("    type: POTION_EFFECT\n");
            w.write("    effect: SPEED\n");
            w.write("    amplifier: 2\n");
        }

        ItemDefinition def = loader.load(file);
        Map<String, PassiveConfig> passives = def.getPassives();
        assertNotNull(passives);
        PassiveConfig passive = passives.get("speed_boost");
        assertNotNull(passive);
        assertEquals("POTION_EFFECT", passive.getType());
        assertEquals("SPEED", passive.getEffect());
        assertEquals(2, passive.getAmplifier());
    }

    @Test
    void testRecipeParsing(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("recipe_item.yml").toFile();
        try (FileWriter w = new FileWriter(file)) {
            w.write("id: recipe_item\n");
            w.write("name: Recipe Item\n");
            w.write("material: IRON_INGOT\n");
            w.write("recipe:\n");
            w.write("  shape:\n");
            w.write("    - 'AAA'\n");
            w.write("    - 'ABA'\n");
            w.write("    - 'AAA'\n");
            w.write("  ingredients:\n");
            w.write("    A: IRON_INGOT\n");
            w.write("    B: STICK\n");
        }

        ItemDefinition def = loader.load(file);
        RecipeConfig recipe = def.getRecipe();
        assertNotNull(recipe);
        assertEquals(List.of("AAA", "ABA", "AAA"), recipe.getShape());
        assertEquals("IRON_INGOT", recipe.getIngredients().get("A"));
        assertEquals("STICK", recipe.getIngredients().get("B"));
    }

    @Test
    void testSetParsing(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("set_item.yml").toFile();
        try (FileWriter w = new FileWriter(file)) {
            w.write("id: set_item\n");
            w.write("name: Set Item\n");
            w.write("material: GOLD_CHESTPLATE\n");
            w.write("set: dragon_armor\n");
        }

        ItemDefinition def = loader.load(file);
        assertEquals("dragon_armor", def.getSet());
    }

    @Test
    void testMissingFieldsHandling(@TempDir Path tempDir) throws IOException {
        // If a yml file has only the bare minimum, the loader should not crash
        File file = tempDir.resolve("minimal.yml").toFile();
        try (FileWriter w = new FileWriter(file)) {
            w.write("id: minimal\n");
        }

        ItemDefinition def = loader.load(file);
        assertEquals("minimal", def.getId());
        assertNull(def.getName());
        assertNull(def.getMaterial());
        assertEquals(0, def.getModel());
        assertNull(def.getLore());
        assertNull(def.getAttributes());
        assertNull(def.getAbilities());
        assertNull(def.getPassives());
        assertNull(def.getSet());
        assertNull(def.getRecipe());
    }

    @Test
    void testInvalidFileThrowsException(@TempDir Path tempDir) {
        File nonExistent = tempDir.resolve("nonexistent.yml").toFile();
        assertThrows(IOException.class, () -> loader.load(nonExistent));
    }

    @Test
    void testEmptyLoreReturnsNull(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("empty_lore.yml").toFile();
        try (FileWriter w = new FileWriter(file)) {
            w.write("id: empty_lore\n");
            w.write("name: Empty Lore\n");
            w.write("material: STONE\n");
            w.write("lore: []\n");
        }

        ItemDefinition def = loader.load(file);
        assertNull(def.getLore());
    }
}
