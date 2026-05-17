package org.mineUGC.core.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ItemDefinition implements UgcAsset {
    private String id;
    private String name;
    private String material;
    private int model;
    private List<String> lore;
    private Map<String, Double> attributes;
    private Map<String, AbilityConfig> abilities;
    private Map<String, PassiveConfig> passives;
    private String set;
    private RecipeConfig recipe;

    @Override public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    @Override public String getType() { return "item"; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getMaterial() { return material; }
    public void setMaterial(String material) { this.material = material; }
    public int getModel() { return model; }
    public void setModel(int model) { this.model = model; }
    public List<String> getLore() { return lore; }
    public void setLore(List<String> lore) { this.lore = lore; }
    public Map<String, Double> getAttributes() { return attributes; }
    public void setAttributes(Map<String, Double> attributes) { this.attributes = attributes; }
    public Map<String, AbilityConfig> getAbilities() { return abilities; }
    public void setAbilities(Map<String, AbilityConfig> abilities) { this.abilities = abilities; }
    public Map<String, PassiveConfig> getPassives() { return passives; }
    public void setPassives(Map<String, PassiveConfig> passives) { this.passives = passives; }
    public String getSet() { return set; }
    public void setSet(String set) { this.set = set; }
    public RecipeConfig getRecipe() { return recipe; }
    public void setRecipe(RecipeConfig recipe) { this.recipe = recipe; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemDefinition that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hashCode(id); }
}
