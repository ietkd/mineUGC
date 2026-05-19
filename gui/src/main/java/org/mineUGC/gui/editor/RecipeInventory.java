package org.mineUGC.gui.editor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.mineUGC.core.message.Messages;
import org.mineUGC.core.model.ItemDefinition;
import org.mineUGC.core.model.RecipeConfig;
import org.mineUGC.gui.fastinv.FastInv;
import org.mineUGC.items.ItemManager;

import java.util.List;
import java.util.Map;

public class RecipeInventory extends FastInv {
    public RecipeInventory(Player player, ItemDefinition def, ItemManager itemManager, GuiListener guiListener) {
        super(54, guiListener.getMessages().get("editor.recipe-title", def.getId()));

        Messages m = guiListener.getMessages();
        RecipeConfig recipe = def.getRecipe();

        // 3x3 shape grid (slots 0-8)
        if (recipe != null && recipe.getShape() != null) {
            List<String> shape = recipe.getShape();
            for (int row = 0; row < 3 && row < shape.size(); row++) {
                String line = shape.get(row);
                for (int col = 0; col < 3 && col < line.length(); col++) {
                    char c = line.charAt(col);
                    int slot = row * 9 + col;
                    Material mat = c == ' ' ? Material.AIR : Material.GRAY_STAINED_GLASS_PANE;
                    String name = c == ' ' ? "§8空" : "§e" + c;
                    String[] lore = {};
                    if (c != ' ' && recipe.getIngredients() != null && recipe.getIngredients().containsKey(String.valueOf(c))) {
                        lore = new String[]{"§7→ " + recipe.getIngredients().get(String.valueOf(c))};
                    }
                    setItem(slot, MainMenuInventory.createItem(mat, name, lore));
                }
            }
        } else {
            for (int i = 0; i < 9; i++) {
                setItem(i, MainMenuInventory.createItem(Material.AIR, ""));
            }
        }

        // Instructions
        setItem(20, MainMenuInventory.createItem(Material.OAK_SIGN, m.get("editor.field-shape"), "§7" + m.get("editor.prompt-shape")), null);
        setItem(24, MainMenuInventory.createItem(Material.MAP, m.get("editor.field-ingredients"), "§7" + m.get("editor.prompt-ingredients")), null);

        // Ingredient display
        if (recipe != null && recipe.getIngredients() != null) {
            int slot = 36;
            for (Map.Entry<String, String> ing : recipe.getIngredients().entrySet()) {
                if (slot >= 45) break;
                setItem(slot, MainMenuInventory.createItem(Material.NAME_TAG,
                        "§e" + ing.getKey(), "§7→ " + ing.getValue()));
                slot++;
            }
        }

        // Buttons
        setItem(30, MainMenuInventory.createItem(Material.LIME_WOOL, m.get("editor.edit-shape"), "§7" + m.get("editor.prompt-shape")),
                e -> {
                    guiListener.openAnvilInput(player, "recipe_shape");
                    guiListener.getEditSession(player.getUniqueId()).ifPresent(session ->
                            session.setReopenAction((p, g) ->
                                    new RecipeInventory(p, def, itemManager, g).open(p)));
                });
        setItem(32, MainMenuInventory.createItem(Material.LIME_WOOL, m.get("editor.edit-ingredients"), "§7" + m.get("editor.prompt-ingredients")),
                e -> {
                    guiListener.openAnvilInput(player, "recipe_ingredients");
                    guiListener.getEditSession(player.getUniqueId()).ifPresent(session ->
                            session.setReopenAction((p, g) ->
                                    new RecipeInventory(p, def, itemManager, g).open(p)));
                });
        setItem(34, MainMenuInventory.createItem(Material.RED_WOOL, m.get("editor.clear-recipe"), ""),
                e -> {
                    def.setRecipe(null);
                    player.sendMessage(m.get("editor.recipe-cleared"));
                    new RecipeInventory(player, def, itemManager, guiListener).open(player);
                });

        setItem(49, MainMenuInventory.createItem(Material.BARRIER, m.get("editor.field-back"), m.get("editor.back-lore")),
                e -> new ItemEditorInventory(player, def, itemManager, guiListener).open(player));
    }
}
