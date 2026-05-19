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

class AbilityEditInventoryTest extends GuiMockTestBase {

    private Player player;

    @BeforeEach
    void setUp() {
        player = server.addPlayer("TestPlayer");
    }

    @Test
    void constructor_shouldCreateInventoryWithCorrectSize() {
        ItemDefinition def = defWithAbility("abil_edit", "right_click");

        AbilityEditInventory inv = new AbilityEditInventory(player, def, "right_click", itemManager, guiListener);
        assertEquals(36, inv.getInventory().getSize());
    }

    @Test
    void constructor_shouldShowAbilityPreviewAtSlot4() {
        ItemDefinition def = defWithAbility("preview_abil", "right_click");

        AbilityEditInventory inv = new AbilityEditInventory(player, def, "right_click", itemManager, guiListener);
        ItemStack preview = inv.getInventory().getItem(4);
        assertNotNull(preview);
        assertEquals(Material.BLAZE_POWDER, preview.getType());
    }

    @Test
    void constructor_shouldShowEditButtons() {
        ItemDefinition def = def("edit_buttons");
        def.setAbilities(new LinkedHashMap<>());
        AbilityConfig ability = new AbilityConfig();
        ability.setType("projectile");
        ability.setCooldown(5);
        ability.setManaCost(10);
        def.getAbilities().put("right_click", ability);

        AbilityEditInventory inv = new AbilityEditInventory(player, def, "right_click", itemManager, guiListener);

        assertNotNull(inv.getInventory().getItem(11)); // Type
        assertNotNull(inv.getInventory().getItem(13)); // Cooldown
        assertNotNull(inv.getInventory().getItem(15)); // Mana
        assertNotNull(inv.getInventory().getItem(31)); // Delete
        assertNotNull(inv.getInventory().getItem(35)); // Back
    }

    @Test
    void constructor_shouldShowAbilityKey() {
        ItemDefinition def = defWithAbility("title_test", "left_click");

        AbilityEditInventory inv = new AbilityEditInventory(player, def, "left_click", itemManager, guiListener);
        assertEquals(36, inv.getInventory().getSize());
    }

    private static ItemDefinition def(String id) {
        ItemDefinition d = new ItemDefinition();
        d.setId(id);
        d.setMaterial("STONE");
        return d;
    }

    private static ItemDefinition defWithAbility(String id, String key) {
        ItemDefinition d = def(id);
        d.setAbilities(new LinkedHashMap<>());
        d.getAbilities().put(key, new AbilityConfig());
        return d;
    }
}
