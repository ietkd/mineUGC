package org.mineUGC.items.crafting;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;
import org.mineUGC.core.model.ItemDefinition;
import org.mineUGC.core.model.RecipeConfig;
import org.mineUGC.items.ItemManager;

import java.util.HashSet;
import java.util.Set;

public class CustomRecipeManager {
    private final Plugin plugin;
    private final ItemManager itemManager;
    private final Set<NamespacedKey> registeredKeys = new HashSet<>();

    public CustomRecipeManager(Plugin plugin, ItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
    }

    public void registerRecipe(ItemDefinition def) {
        RecipeConfig recipe = def.getRecipe();
        if (recipe == null || !recipe.isValid()) return;

        NamespacedKey key = new NamespacedKey(plugin, "ugc_" + def.getId());

        ItemStack result = itemManager.createItemStack(def);
        ShapedRecipe shaped = new ShapedRecipe(key, result);
        shaped.shape(recipe.getShape().toArray(new String[0]));

        for (var entry : recipe.getIngredients().entrySet()) {
            Material mat = Material.getMaterial(entry.getValue().toUpperCase());
            if (mat != null) {
                shaped.setIngredient(entry.getKey().charAt(0), mat);
            }
        }

        Bukkit.addRecipe(shaped);
        registeredKeys.add(key);
    }

    public void unregisterAll() {
        for (NamespacedKey key : registeredKeys) {
            Bukkit.removeRecipe(key);
        }
        registeredKeys.clear();
    }

    public void reloadRecipes() {
        unregisterAll();
        for (ItemDefinition def : itemManager.getAllDefinitions()) {
            registerRecipe(def);
        }
    }
}
