package org.mineUGC.storage.yaml;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.mineUGC.core.model.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class YamlItemLoader {

    public ItemDefinition load(File file) throws IOException {
        if (!file.exists()) {
            throw new IOException("File not found: " + file.getAbsolutePath());
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        return parse(config);
    }

    public ItemDefinition parse(ConfigurationSection config) {
        ItemDefinition def = new ItemDefinition();

        def.setId(config.getString("id"));
        def.setName(config.getString("name"));
        def.setMaterial(config.getString("material"));
        def.setModel(config.getInt("model", 0));

        List<String> lore = config.getStringList("lore");
        def.setLore(lore.isEmpty() ? null : lore);

        // attributes
        ConfigurationSection attrSection = config.getConfigurationSection("attributes");
        if (attrSection != null) {
            Map<String, Double> attrs = new LinkedHashMap<>();
            for (String key : attrSection.getKeys(false)) {
                attrs.put(key, attrSection.getDouble(key));
            }
            def.setAttributes(attrs);
        }

        // abilities
        ConfigurationSection abilSection = config.getConfigurationSection("abilities");
        if (abilSection != null) {
            Map<String, AbilityConfig> abilities = new LinkedHashMap<>();
            for (String key : abilSection.getKeys(false)) {
                ConfigurationSection cfg = abilSection.getConfigurationSection(key);
                if (cfg != null) {
                    abilities.put(key, parseAbilityConfig(cfg));
                }
            }
            def.setAbilities(abilities);
        }

        // passives
        ConfigurationSection passSection = config.getConfigurationSection("passives");
        if (passSection != null) {
            Map<String, PassiveConfig> passives = new LinkedHashMap<>();
            for (String key : passSection.getKeys(false)) {
                ConfigurationSection cfg = passSection.getConfigurationSection(key);
                if (cfg != null) {
                    passives.put(key, parsePassiveConfig(cfg));
                }
            }
            def.setPassives(passives);
        }

        def.setSet(config.getString("set"));

        // recipe
        ConfigurationSection recipeSection = config.getConfigurationSection("recipe");
        if (recipeSection != null) {
            def.setRecipe(parseRecipeConfig(recipeSection));
        }

        return def;
    }

    private AbilityConfig parseAbilityConfig(ConfigurationSection section) {
        AbilityConfig config = new AbilityConfig();
        config.setType(section.getString("type"));
        config.setCooldown(section.getInt("cooldown", 0));
        config.setManaCost(section.getInt("mana_cost", 0));
        Map<String, Object> params = new HashMap<>();
        for (String key : section.getKeys(false)) {
            if (!key.equals("type") && !key.equals("cooldown") && !key.equals("mana_cost")) {
                params.put(key, section.get(key));
            }
        }
        config.setParams(params);
        return config;
    }

    private PassiveConfig parsePassiveConfig(ConfigurationSection section) {
        PassiveConfig config = new PassiveConfig();
        config.setType(section.getString("type"));
        config.setEffect(section.getString("effect"));
        config.setAmplifier(section.getInt("amplifier", 0));
        Map<String, Object> params = new HashMap<>();
        for (String key : section.getKeys(false)) {
            if (!key.equals("type") && !key.equals("effect") && !key.equals("amplifier")) {
                params.put(key, section.get(key));
            }
        }
        config.setParams(params);
        return config;
    }

    private RecipeConfig parseRecipeConfig(ConfigurationSection section) {
        RecipeConfig config = new RecipeConfig();
        config.setShape(section.getStringList("shape"));
        ConfigurationSection ingSection = section.getConfigurationSection("ingredients");
        if (ingSection != null) {
            Map<String, String> ingredients = new LinkedHashMap<>();
            for (String key : ingSection.getKeys(false)) {
                ingredients.put(key, ingSection.getString(key));
            }
            config.setIngredients(ingredients);
        }
        return config;
    }
}
