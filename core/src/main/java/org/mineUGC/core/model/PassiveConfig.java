package org.mineUGC.core.model;

import java.util.HashMap;
import java.util.Map;

public class PassiveConfig {
    private String type;
    private String effect;
    private int amplifier;
    private Map<String, Object> params = new HashMap<>();

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getEffect() { return effect; }
    public void setEffect(String effect) { this.effect = effect; }
    public int getAmplifier() { return amplifier; }
    public void setAmplifier(int amplifier) { this.amplifier = amplifier; }
    public Map<String, Object> getParams() { return params; }
    public void setParams(Map<String, Object> params) { this.params = params; }
}
