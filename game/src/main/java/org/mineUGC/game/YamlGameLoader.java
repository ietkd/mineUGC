package org.mineUGC.game;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.mineUGC.core.model.ItemDefinition;
import org.mineUGC.game.model.GameDefinition;

import java.io.File;
import java.io.IOException;

public class YamlGameLoader {

    public GameDefinition load(File file) throws IOException {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        return parse(config);
    }

    public GameDefinition parse(ConfigurationSection config) {
        GameDefinition def = new GameDefinition();
        def.setId(config.getString("id"));
        def.setName(config.getString("name"));
        def.setMapWorldName(config.getString("map_world"));
        def.setMaxPlayers(config.getInt("max_players", 16));
        def.setMinPlayers(config.getInt("min_players", 2));
        def.setTeamMode(config.getString("team_mode", "solo"));
        def.setTeamSize(config.getInt("team_size", 1));
        def.setWarmupTime(config.getInt("warmup_time", 60));
        def.setTimeLimit(config.getInt("time_limit", 1800));
        def.setPvp(config.getBoolean("pvp", true));
        def.setFriendlyFire(config.getBoolean("friendly_fire", false));
        def.setCircleEnabled(config.getBoolean("circle.enabled", true));
        def.setCircleInitialDelay(config.getInt("circle.initial_delay", 300));
        def.setCircleShrinkDuration(config.getInt("circle.shrink_duration", 120));
        def.setCircleDamagePerSec(config.getDouble("circle.damage_per_sec", 1.0));
        def.setWinCondition(config.getString("win_condition", "last_standing"));
        def.setAllowedItems(config.getStringList("allowed_items"));
        def.setRuleSetId(config.getString("rule_set"));
        def.setDeviceIds(config.getStringList("devices"));
        def.setObjectIds(config.getStringList("objects"));
        return def;
    }

    public void save(GameDefinition def, File file) throws IOException {
        YamlConfiguration config = new YamlConfiguration();
        config.set("id", def.getId());
        config.set("name", def.getName());
        config.set("map_world", def.getMapWorldName());
        config.set("max_players", def.getMaxPlayers());
        config.set("min_players", def.getMinPlayers());
        config.set("team_mode", def.getTeamMode());
        config.set("team_size", def.getTeamSize());
        config.set("warmup_time", def.getWarmupTime());
        config.set("time_limit", def.getTimeLimit());
        config.set("pvp", def.isPvp());
        config.set("friendly_fire", def.isFriendlyFire());
        config.set("circle.enabled", def.isCircleEnabled());
        config.set("circle.initial_delay", def.getCircleInitialDelay());
        config.set("circle.shrink_duration", def.getCircleShrinkDuration());
        config.set("circle.damage_per_sec", def.getCircleDamagePerSec());
        config.set("win_condition", def.getWinCondition());
        config.set("allowed_items", def.getAllowedItems());
        if (def.getRuleSetId() != null) config.set("rule_set", def.getRuleSetId());
        if (!def.getDeviceIds().isEmpty()) config.set("devices", def.getDeviceIds());
        if (!def.getObjectIds().isEmpty()) config.set("objects", def.getObjectIds());
        config.save(file);
    }
}
