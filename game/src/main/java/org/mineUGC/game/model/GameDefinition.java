package org.mineUGC.game.model;

import org.mineUGC.core.model.UgcAsset;

import java.util.*;

public class GameDefinition implements UgcAsset {
    private String id;
    private String name;
    private String mapWorldName;
    private int maxPlayers = 16;
    private int minPlayers = 2;
    private String teamMode = "solo"; // solo, duo, squad
    private int teamSize = 1;
    private int warmupTime = 60;
    private int timeLimit = 1800;
    private boolean pvp = true;
    private boolean friendlyFire = false;

    // Safe zone settings
    private boolean circleEnabled = true;
    private int circleInitialDelay = 300;
    private int circleShrinkDuration = 120;
    private double circleDamagePerSec = 1.0;

    // Win condition
    private String winCondition = "last_standing"; // last_standing, score, timed

    // Allowed items (* = all)
    private List<String> allowedItems = new ArrayList<>();

    // References to rule/devices/objects (future phases)
    private String ruleSetId;
    private List<String> deviceIds = new ArrayList<>();
    private List<String> objectIds = new ArrayList<>();

    @Override public String getId() { return id; }
    @Override public String getType() { return "game"; }

    // Getters and setters for all fields
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getMapWorldName() { return mapWorldName; }
    public void setMapWorldName(String mapWorldName) { this.mapWorldName = mapWorldName; }
    public int getMaxPlayers() { return maxPlayers; }
    public void setMaxPlayers(int maxPlayers) { this.maxPlayers = maxPlayers; }
    public int getMinPlayers() { return minPlayers; }
    public void setMinPlayers(int minPlayers) { this.minPlayers = minPlayers; }
    public String getTeamMode() { return teamMode; }
    public void setTeamMode(String teamMode) { this.teamMode = teamMode; }
    public int getTeamSize() { return teamSize; }
    public void setTeamSize(int teamSize) { this.teamSize = teamSize; }
    public int getWarmupTime() { return warmupTime; }
    public void setWarmupTime(int warmupTime) { this.warmupTime = warmupTime; }
    public int getTimeLimit() { return timeLimit; }
    public void setTimeLimit(int timeLimit) { this.timeLimit = timeLimit; }
    public boolean isPvp() { return pvp; }
    public void setPvp(boolean pvp) { this.pvp = pvp; }
    public boolean isFriendlyFire() { return friendlyFire; }
    public void setFriendlyFire(boolean friendlyFire) { this.friendlyFire = friendlyFire; }
    public boolean isCircleEnabled() { return circleEnabled; }
    public void setCircleEnabled(boolean circleEnabled) { this.circleEnabled = circleEnabled; }
    public int getCircleInitialDelay() { return circleInitialDelay; }
    public void setCircleInitialDelay(int delay) { this.circleInitialDelay = delay; }
    public int getCircleShrinkDuration() { return circleShrinkDuration; }
    public void setCircleShrinkDuration(int duration) { this.circleShrinkDuration = duration; }
    public double getCircleDamagePerSec() { return circleDamagePerSec; }
    public void setCircleDamagePerSec(double dmg) { this.circleDamagePerSec = dmg; }
    public String getWinCondition() { return winCondition; }
    public void setWinCondition(String winCondition) { this.winCondition = winCondition; }
    public List<String> getAllowedItems() { return allowedItems; }
    public void setAllowedItems(List<String> items) { this.allowedItems = items; }
    public String getRuleSetId() { return ruleSetId; }
    public void setRuleSetId(String id) { this.ruleSetId = id; }
    public List<String> getDeviceIds() { return deviceIds; }
    public void setDeviceIds(List<String> ids) { this.deviceIds = ids; }
    public List<String> getObjectIds() { return objectIds; }
    public void setObjectIds(List<String> ids) { this.objectIds = ids; }
}
