package org.mineUGC.core.model;

import java.util.HashMap;
import java.util.Map;

public class AbilityConfig {
    private String type;
    private int cooldown;
    private int manaCost;
    private Map<String, Object> params = new HashMap<>();

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public int getCooldown() { return cooldown; }
    public void setCooldown(int cooldown) { this.cooldown = cooldown; }
    public int getManaCost() { return manaCost; }
    public void setManaCost(int manaCost) { this.manaCost = manaCost; }
    public Map<String, Object> getParams() { return params; }
    public void setParams(Map<String, Object> params) { this.params = params; }

    @SuppressWarnings("unchecked")
    public <T> T param(String key, T fallback) {
        T val = (T) params.get(key);
        return val != null ? val : fallback;
    }
}
