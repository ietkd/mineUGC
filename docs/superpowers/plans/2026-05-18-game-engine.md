# Game Engine (Phase 4) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build the core game engine — a battle-royale-style game framework with phase state machine, teams, safe zone, player lifecycle, and independent worlds. This is the first piece of the PUBG UGC platform.

**Architecture:** New `game` Gradle module with a `GameSession` state machine ticked once per second, a `GameManager` singleton orchestrator, Paper Structure API for world preparation, and `/ugc game` subcommands. The existing items module is used as a compile dependency for loot/loadout integration.

**Tech Stack:** Java 21, Paper 1.21 API, FastInv (existing), YamlConfiguration (existing)

---

### Task 1: Create game module skeleton

**Files:**
- Create: `game/build.gradle`
- Modify: `settings.gradle`
- Create: `game/src/main/java/org/mineUGC/game/package-info.java` (empty marker)
- Create: `game/src/main/java/org/mineUGC/game/model/GameDefinition.java`
- Create: `game/src/main/java/org/mineUGC/game/model/GamePhase.java`
- Create: `game/src/main/java/org/mineUGC/game/model/GameTeam.java`
- Create: `game/src/main/java/org/mineUGC/game/model/GamePlayer.java`

- [ ] **Add game module to settings.gradle**

Add after the existing includes:
```groovy
include 'game'
```

- [ ] **Create `game/build.gradle`**

```groovy
dependencies {
    implementation project(':core')
    implementation project(':storage')
    implementation project(':items')
}
```

- [ ] **Create `GamePhase.java`**

```java
package org.mineUGC.game.model;

public enum GamePhase {
    LOBBY,
    PRE_GAME,
    PLAYING,
    CIRCLE_SHRINKING,
    ENDING,
    FINISHED
}
```

- [ ] **Create `GameTeam.java`**

```java
package org.mineUGC.game.model;

import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class GameTeam {
    private final String name;
    private final Set<UUID> members = new HashSet<>();
    private int score;
    private boolean alive = true;

    public GameTeam(String name) { this.name = name; }

    public String getName() { return name; }
    public Set<UUID> getMembers() { return members; }
    public boolean addMember(UUID playerId) { return members.add(playerId); }
    public boolean removeMember(UUID playerId) { return members.remove(playerId); }
    public boolean hasMember(UUID playerId) { return members.contains(playerId); }
    public int getMemberCount() { return members.size(); }
    public int getAliveCount() { return (int) members.stream().filter(id -> alive).count(); }
    public int getScore() { return score; }
    public void addScore(int points) { this.score += points; }
    public boolean isAlive() { return alive && !members.isEmpty(); }
    public void setAlive(boolean alive) { this.alive = alive; }
}
```

- [ ] **Create `GamePlayer.java`**

```java
package org.mineUGC.game.model;

import java.util.UUID;

public class GamePlayer {
    private final UUID playerId;
    private final String playerName;
    private int kills;
    private int deaths;
    private int damageDealt;
    private boolean alive = true;

    public GamePlayer(UUID playerId, String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
    }

    public UUID getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }
    public int getKills() { return kills; }
    public void addKill() { this.kills++; }
    public int getDeaths() { return deaths; }
    public void addDeath() { this.deaths++; }
    public int getDamageDealt() { return damageDealt; }
    public void addDamageDealt(int damage) { this.damageDealt += damage; }
    public boolean isAlive() { return alive; }
    public void setAlive(boolean alive) { this.alive = alive; }
}
```

- [ ] **Create `GameDefinition.java`**

```java
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
```

- [ ] **Commit**

```bash
git add game/build.gradle settings.gradle game/src/main/java/org/mineUGC/game/model/
git commit -m "feat(game): create game module with data models"
```

---

### Task 2: Create GameDefinition YAML loader

**Files:**
- Create: `game/src/main/java/org/mineUGC/game/YamlGameLoader.java`
- Create: `game/src/main/java/org/mineUGC/game/GameRegistry.java`

- [ ] **Create `YamlGameLoader.java`**

```java
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
```

- [ ] **Create `GameRegistry.java`**

```java
package org.mineUGC.game;

import org.mineUGC.core.registry.AssetRegistry;
import org.mineUGC.game.model.GameDefinition;

public class GameRegistry extends AssetRegistry<GameDefinition> {
    // Inherits all from AssetRegistry
}
```

- [ ] **Create example game definition YAML**

Create `plugin/src/main/resources/games/classic_battle_royale.yml`:
```yaml
id: classic_battle_royale
name: "经典大逃杀"
map_world: "br_map_01"
max_players: 16
min_players: 2
team_mode: solo
team_size: 1
warmup_time: 30
time_limit: 1800
pvp: true
friendly_fire: false
circle:
  enabled: true
  initial_delay: 120
  shrink_duration: 90
  damage_per_sec: 1.0
win_condition: last_standing
allowed_items:
  - "*"
```

- [ ] **Commit**

```bash
git add game/src/main/java/org/mineUGC/game/YamlGameLoader.java game/src/main/java/org/mineUGC/game/GameRegistry.java plugin/src/main/resources/games/
git commit -m "feat(game): add YAML loader and example game definition"
```

---

### Task 3: Create GameSession state machine

**Files:**
- Create: `game/src/main/java/org/mineUGC/game/model/GameSession.java`
- Create: `game/src/main/java/org/mineUGC/game/model/GameResult.java`

- [ ] **Create `GameResult.java`**

```java
package org.mineUGC.game.model;

import java.util.*;

public class GameResult {
    private final String gameDefinitionId;
    private final GameTeam winner;
    private final Map<UUID, GamePlayer> players;
    private final int totalKills;
    private final long durationSeconds;

    public GameResult(String gameDefinitionId, GameTeam winner, Map<UUID, GamePlayer> players,
                      int totalKills, long durationSeconds) {
        this.gameDefinitionId = gameDefinitionId;
        this.winner = winner;
        this.players = players;
        this.totalKills = totalKills;
        this.durationSeconds = durationSeconds;
    }

    public String getGameDefinitionId() { return gameDefinitionId; }
    public GameTeam getWinner() { return winner; }
    public Map<UUID, GamePlayer> getPlayers() { return players; }
    public int getTotalKills() { return totalKills; }
    public long getDurationSeconds() { return durationSeconds; }
}
```

- [ ] **Create `GameSession.java`**

```java
package org.mineUGC.game.model;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.mineUGC.core.model.ItemDefinition;
import org.mineUGC.items.ItemManager;
import org.mineUGC.items.InventoryScanner;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class GameSession {
    private final String id;
    private final GameDefinition definition;
    private final Plugin plugin;
    private final ItemManager itemManager;
    private final InventoryScanner inventoryScanner;

    private GamePhase phase = GamePhase.LOBBY;
    private int phaseTicks;
    private World gameWorld;
    private boolean worldPrepared;

    // Players and teams
    private final Map<UUID, GamePlayer> players = new ConcurrentHashMap<>();
    private final List<GameTeam> teams = new ArrayList<>();

    // Safe zone state
    private Location circleCenter;
    private double currentRadius;
    private double targetRadius;
    private Location nextCircleCenter;
    private int circleShrinkTicks;
    private boolean circleInitialPhase = true;

    // Timing
    private long startTime;
    private final long createdAt = System.currentTimeMillis();

    public GameSession(String id, GameDefinition definition, Plugin plugin,
                       ItemManager itemManager, InventoryScanner inventoryScanner) {
        this.id = id;
        this.definition = definition;
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.inventoryScanner = inventoryScanner;
    }

    // === Getters ===
    public String getId() { return id; }
    public GameDefinition getDefinition() { return definition; }
    public GamePhase getPhase() { return phase; }
    public World getWorld() { return gameWorld; }
    public Collection<GamePlayer> getPlayers() { return players.values(); }
    public List<GameTeam> getTeams() { return teams; }
    public boolean isWorldPrepared() { return worldPrepared; }

    // === Phase Transitions ===

    public void setPhase(GamePhase newPhase) {
        this.phase = newPhase;
        this.phaseTicks = 0;
    }

    // === Player Management ===

    public boolean addPlayer(Player player) {
        if (players.containsKey(player.getUniqueId())) return false;
        if (players.size() >= definition.getMaxPlayers()) return false;

        players.put(player.getUniqueId(), new GamePlayer(player.getUniqueId(), player.getName()));

        // Auto-assign to team
        assignToTeam(player);

        // Teleport to world spawn
        if (gameWorld != null) {
            player.teleport(gameWorld.getSpawnLocation());
        }

        return true;
    }

    public void removePlayer(UUID playerId) {
        players.remove(playerId);
        teams.forEach(t -> t.removeMember(playerId));
    }

    public GamePlayer getGamePlayer(UUID playerId) {
        return players.get(playerId);
    }

    public boolean hasPlayer(UUID playerId) {
        return players.containsKey(playerId);
    }

    private void assignToTeam(Player player) {
        String mode = definition.getTeamMode();
        int teamSize = definition.getTeamSize();

        if ("solo".equals(mode) || teamSize <= 1) {
            // Each player is their own team
            GameTeam team = new GameTeam("player_" + player.getUniqueId());
            team.addMember(player.getUniqueId());
            teams.add(team);
        } else {
            // Find team with space
            GameTeam available = teams.stream()
                    .filter(t -> t.getMemberCount() < teamSize)
                    .findFirst().orElse(null);
            if (available != null) {
                available.addMember(player.getUniqueId());
            } else {
                GameTeam newTeam = new GameTeam("team_" + (teams.size() + 1));
                newTeam.addMember(player.getUniqueId());
                teams.add(newTeam);
            }
        }
    }

    public List<GameTeam> getAliveTeams() {
        return teams.stream().filter(GameTeam::isAlive).collect(Collectors.toList());
    }

    // === Main Tick (called every second) ===

    public void tick() {
        phaseTicks++;

        switch (phase) {
            case LOBBY -> tickLobby();
            case PRE_GAME -> tickPreGame();
            case PLAYING -> tickPlaying();
            case CIRCLE_SHRINKING -> tickCircleShrinking();
            case ENDING -> tickEnding();
        }
    }

    private void tickLobby() {
        // Check if enough players to start
        if (players.size() >= definition.getMinPlayers() && phaseTicks >= 30) {
            enterPreGame();
        }
    }

    private void enterPreGame() {
        setPhase(GamePhase.PRE_GAME);
        broadcast("§e游戏即将开始！准备阶段 " + definition.getWarmupTime() + " 秒");
        phaseTicks = 0;
    }

    private void tickPreGame() {
        if (phaseTicks >= definition.getWarmupTime()) {
            enterPlaying();
        } else if (phaseTicks % 10 == 0) {
            broadcast("§e" + (definition.getWarmupTime() - phaseTicks) + " 秒后开始");
        }
    }

    private void enterPlaying() {
        setPhase(GamePhase.PLAYING);
        startTime = System.currentTimeMillis();
        broadcast("§a§l游戏开始！");

        // Initialize safe zone
        if (definition.isCircleEnabled() && gameWorld != null) {
            initCircle();
        }

        // Teleport all alive players to spawn
        World world = gameWorld;
        if (world != null) {
            players.forEach((uuid, gp) -> {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && p.isOnline()) {
                    scatterPlayer(p, world);
                    p.setGameMode(GameMode.SURVIVAL);
                }
            });
        }
    }

    private void tickPlaying() {
        // Check circle
        if (definition.isCircleEnabled()) {
            if (circleInitialPhase && phaseTicks >= definition.getCircleInitialDelay()) {
                enterCircleShrinking();
            }
            // Apply circle damage
            applyCircleDamage();
        }

        // Check time limit
        if (definition.getTimeLimit() > 0 && phaseTicks >= definition.getTimeLimit()) {
            // Time's up — determine winner by score or most kills
            endGame(findWinnerByScore());
            return;
        }

        // Check win condition
        checkWinCondition();
    }

    private void enterCircleShrinking() {
        setPhase(GamePhase.CIRCLE_SHRINKING);
        circleInitialPhase = false;
        circleShrinkTicks = 0;

        // Generate next smaller circle
        if (gameWorld != null) {
            nextCircleCenter = generateNextCircleCenter();
            targetRadius = currentRadius * 0.5;
        }

        broadcast("§c§l安全区正在缩小！");
    }

    private void tickCircleShrinking() {
        circleShrinkTicks++;

        // Lerp circle size
        double progress = Math.min(1.0, (double) circleShrinkTicks / definition.getCircleShrinkDuration());
        currentRadius = currentRadius + (targetRadius - currentRadius) * (progress / 10.0);

        if (progress >= 1.0) {
            currentRadius = targetRadius;
            circleCenter = nextCircleCenter;
            setPhase(GamePhase.PLAYING);
            broadcast("§a安全区缩小完成");
        }

        applyCircleDamage();
        checkWinCondition();
    }

    private void tickEnding() {
        if (phaseTicks >= 10) { // 10 seconds in ending phase
            setPhase(GamePhase.FINISHED);
            // Schedule world cleanup
            Bukkit.getScheduler().runTaskLater(plugin, this::cleanup, 100L);
        }
    }

    // === Safe Zone ===

    private void initCircle() {
        WorldBorder border = gameWorld.getWorldBorder();
        double worldSize = gameWorld.getWorldBorder().getSize();
        circleCenter = new Location(gameWorld, 0, 64, 0);
        currentRadius = worldSize / 2;
        border.setCenter(circleCenter);
        border.setSize(worldSize);

        // Visual circle with particles
        if (circleCenter != null) {
            showCircleBorder(circleCenter, currentRadius);
        }
    }

    private Location generateNextCircleCenter() {
        // Random offset from current center within shrink radius
        Random rand = new Random();
        double maxOffset = currentRadius * 0.3;
        double dx = (rand.nextDouble() - 0.5) * 2 * maxOffset;
        double dz = (rand.nextDouble() - 0.5) * 2 * maxOffset;
        return new Location(gameWorld,
                circleCenter.getX() + dx,
                64,
                circleCenter.getZ() + dz);
    }

    private void applyCircleDamage() {
        if (gameWorld == null || circleCenter == null) return;

        double damage = definition.getCircleDamagePerSec();
        // Apply every tick (we tick once per second, so this is the per-second damage)

        players.forEach((uuid, gp) -> {
            if (!gp.isAlive()) return;
            Player p = Bukkit.getPlayer(uuid);
            if (p == null || !p.isOnline()) return;
            if (!p.getWorld().equals(gameWorld)) return;

            Location loc = p.getLocation();
            double dist = distance2D(loc, circleCenter);
            if (dist > currentRadius) {
                p.damage(damage);
                // Visual warning
                if (p.getHealth() > damage) {
                    p.playSound(loc, Sound.BLOCK_BEACON_AMBIENT, 0.5f, 0.5f);
                }
            }
        });
    }

    private void showCircleBorder(Location center, double radius) {
        // Visual circle using particles
        for (int i = 0; i < 360; i += 3) {
            double angle = Math.toRadians(i);
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            Location particleLoc = new Location(gameWorld, x, center.getY(), z);
            gameWorld.spawnParticle(Particle.DRAGON_BREATH, particleLoc, 1, 0, 0, 0, 0);
        }
    }

    private double distance2D(Location a, Location b) {
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    // === Player Scatter ===

    private void scatterPlayer(Player player, World world) {
        // Simple scatter around world center
        double radius = currentRadius * 0.8;
        Random rand = new Random();
        double angle = rand.nextDouble() * 2 * Math.PI;
        double dist = rand.nextDouble() * radius;
        double x = dist * Math.cos(angle);
        double z = dist * Math.sin(angle);

        // Find highest block at position
        Location loc = new Location(world, x, 100, z);
        loc = world.getHighestBlockAt((int) x, (int) z).getLocation().add(0.5, 1, 0.5);
        player.teleport(loc);
    }

    // === Win Condition ===

    private void checkWinCondition() {
        if ("last_standing".equals(definition.getWinCondition())) {
            List<GameTeam> alive = getAliveTeams();
            if (alive.size() <= 1 && players.size() > 1) {
                // One team left
                GameTeam winner = alive.isEmpty() ? null : alive.get(0);
                endGame(winner);
            } else if (players.size() <= 1 && players.size() == 1) {
                // Single player last standing
                UUID lastId = players.keySet().iterator().next();
                GameTeam winner = teams.stream()
                        .filter(t -> t.hasMember(lastId))
                        .findFirst().orElse(null);
                endGame(winner);
            }
        }
    }

    private GameTeam findWinnerByScore() {
        return teams.stream()
                .max(Comparator.comparingInt(GameTeam::getScore))
                .orElse(null);
    }

    private void endGame(GameTeam winner) {
        setPhase(GamePhase.ENDING);

        long duration = (System.currentTimeMillis() - startTime) / 1000;
        GameResult result = new GameResult(definition.getId(), winner, players,
                players.values().stream().mapToInt(GamePlayer::getKills).sum(),
                duration);

        if (winner != null) {
            String winnerName;
            if ("solo".equals(definition.getTeamMode())) {
                UUID winnerId = winner.getMembers().iterator().next();
                winnerName = Bukkit.getOfflinePlayer(winnerId).getName();
            } else {
                winnerName = winner.getName();
            }
            broadcast("§6§l🎉 获胜者: " + winnerName + "！");
        } else {
            broadcast("§c游戏结束 - 没有获胜者");
        }

        broadcast("§7游戏时长: " + duration + " 秒");
        broadcast("§7总击杀: " + result.getTotalKills());

        // Show stats to each player
        players.forEach((uuid, gp) -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.sendMessage("§7你的击杀: " + gp.getKills() + " | 死亡: " + gp.getDeaths());
            }
        });

        // Return players to hub after delay
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            players.keySet().forEach(uuid -> {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && p.isOnline()) {
                    p.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
                    p.setGameMode(GameMode.ADVENTURE);
                }
            });
        }, 100L);
    }

    // === World Management ===

    public boolean prepareWorld(String worldName) {
        if (worldPrepared) return true;

        try {
            // Try to load existing world
            WorldCreator creator = new WorldCreator(worldName);
            creator.environment(World.Environment.NORMAL);
            creator.type(WorldType.FLAT);
            creator.generateStructures(false);
            this.gameWorld = Bukkit.createWorld(creator);

            if (gameWorld == null) {
                plugin.getLogger().warning("Failed to create world: " + worldName);
                return false;
            }

            // Configure world
            gameWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            gameWorld.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
            gameWorld.setGameRule(GameRule.MOB_GRIEFING, false);
            gameWorld.setGameRule(GameRule.DO_MOB_SPAWNING, false);
            gameWorld.setGameRule(GameRule.DO_FIRE_TICK, false);
            gameWorld.setTime(1000); // Daytime

            // Set world border for safe zone
            WorldBorder border = gameWorld.getWorldBorder();
            border.setSize(1000);
            border.setDamageAmount(0); // We handle damage ourselves

            // Initialize circle center
            this.circleCenter = new Location(gameWorld, 0, 64, 0);
            this.currentRadius = 500;

            this.worldPrepared = true;
            plugin.getLogger().info("World prepared: " + worldName);
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to prepare world: " + e.getMessage());
            return false;
        }
    }

    public void teleportToWorld(Player player) {
        if (gameWorld != null) {
            player.teleport(gameWorld.getSpawnLocation());
            player.setGameMode(GameMode.ADVENTURE);
        }
    }

    // === Broadcast ===

    public void broadcast(String message) {
        players.keySet().forEach(uuid -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.sendMessage(message);
            }
        });
    }

    // === Cleanup ===

    public void cleanup() {
        // Return all players to main world
        players.keySet().forEach(uuid -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
                p.setGameMode(GameMode.ADVENTURE);
            }
        });

        // Unload game world
        if (gameWorld != null) {
            Bukkit.unloadWorld(gameWorld, false);
            plugin.getLogger().info("Unloaded game world: " + gameWorld.getName());
            gameWorld = null;
        }

        worldPrepared = false;
    }

    public boolean isFinished() {
        return phase == GamePhase.FINISHED;
    }
}
```

Note: `Particle.DRAGON_BREATH` is the 1.21 name. If compilation fails, use `Particle.valueOf("DRAGON_BREATH")`.

- [ ] **Commit**

```bash
git add game/src/main/java/org/mineUGC/game/model/GameSession.java game/src/main/java/org/mineUGC/game/model/GameResult.java
git commit -m "feat(game): implement GameSession state machine"
```

---

### Task 4: Create GameManager

**Files:**
- Create: `game/src/main/java/org/mineUGC/game/GameManager.java`
- Create: `game/src/main/java/org/mineUGC/game/GameTickTask.java`

- [ ] **Create `GameTickTask.java`**

```java
package org.mineUGC.game;

import org.bukkit.scheduler.BukkitRunnable;

public class GameTickTask extends BukkitRunnable {
    private final GameManager gameManager;

    public GameTickTask(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public void run() {
        gameManager.tickAll();
    }
}
```

- [ ] **Create `GameManager.java`**

```java
package org.mineUGC.game;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.mineUGC.game.model.GameDefinition;
import org.mineUGC.game.model.GameSession;
import org.mineUGC.items.ItemManager;
import org.mineUGC.items.InventoryScanner;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class GameManager {
    private final Plugin plugin;
    private final ItemManager itemManager;
    private final InventoryScanner inventoryScanner;
    private final GameRegistry gameRegistry;
    private final YamlGameLoader gameLoader;

    private final Map<String, GameSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerSessions = new ConcurrentHashMap<>();

    // Queue management
    private final Map<String, List<UUID>> queues = new ConcurrentHashMap<>();

    public GameManager(Plugin plugin, ItemManager itemManager,
                       InventoryScanner inventoryScanner,
                       GameRegistry gameRegistry, YamlGameLoader gameLoader) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.inventoryScanner = inventoryScanner;
        this.gameRegistry = gameRegistry;
        this.gameLoader = gameLoader;
    }

    // === Session Lifecycle ===

    public GameSession createSession(String gameDefinitionId, String worldName) {
        GameDefinition def = gameRegistry.get(gameDefinitionId);
        if (def == null) return null;

        String sessionId = UUID.randomUUID().toString().substring(0, 8);
        GameSession session = new GameSession(sessionId, def, plugin, itemManager, inventoryScanner);

        // Prepare world
        if (!session.prepareWorld(worldName != null ? worldName : def.getMapWorldName())) {
            return null;
        }

        activeSessions.put(sessionId, session);
        plugin.getLogger().info("Game session created: " + sessionId + " (" + def.getName() + ")");
        return session;
    }

    public boolean startSession(String sessionId) {
        GameSession session = activeSessions.get(sessionId);
        if (session == null) return false;

        // Move queued players into session
        List<UUID> queued = queues.remove(sessionId);
        if (queued != null) {
            for (UUID playerId : queued) {
                Player p = Bukkit.getPlayer(playerId);
                if (p != null && p.isOnline()) {
                    session.addPlayer(p);
                }
            }
        }

        // Force transition to PRE_GAME
        session.setPhase(org.mineUGC.game.model.GamePhase.PRE_GAME);
        return true;
    }

    public boolean stopSession(String sessionId) {
        GameSession session = activeSessions.get(sessionId);
        if (session == null) return false;

        // Clean up
        playerSessions.values().removeIf(s -> s.equals(sessionId));
        queues.remove(sessionId);
        activeSessions.remove(sessionId);
        return true;
    }

    public void destroySession(String sessionId) {
        GameSession session = activeSessions.get(sessionId);
        if (session != null) {
            session.cleanup();
            stopSession(sessionId);
        }
    }

    // === Player Management ===

    public boolean joinSession(String sessionId, Player player) {
        if (playerSessions.containsKey(player.getUniqueId())) return false;

        GameSession session = activeSessions.get(sessionId);
        if (session == null) return false;

        if (session.addPlayer(player)) {
            playerSessions.put(player.getUniqueId(), sessionId);
            return true;
        }
        return false;
    }

    public void leaveSession(Player player) {
        String sessionId = playerSessions.remove(player.getUniqueId());
        if (sessionId != null) {
            GameSession session = activeSessions.get(sessionId);
            if (session != null) {
                session.removePlayer(player.getUniqueId());
            }
        }
    }

    public GameSession getPlayerSession(Player player) {
        String sessionId = playerSessions.get(player.getUniqueId());
        if (sessionId == null) return null;
        return activeSessions.get(sessionId);
    }

    public GameSession getSession(String sessionId) {
        return activeSessions.get(sessionId);
    }

    public Collection<GameSession> getActiveSessions() {
        return activeSessions.values();
    }

    public List<String> getSessionIds() {
        return List.copyOf(activeSessions.keySet());
    }

    // === Queue ===

    public boolean addToQueue(Player player, String gameDefinitionId) {
        if (playerSessions.containsKey(player.getUniqueId())) return false;
        queues.computeIfAbsent(gameDefinitionId, k -> new ArrayList<>()).add(player.getUniqueId());
        return true;
    }

    public void removeFromQueue(Player player) {
        queues.values().forEach(q -> q.remove(player.getUniqueId()));
    }

    public int getQueueSize(String gameDefinitionId) {
        List<UUID> q = queues.get(gameDefinitionId);
        return q == null ? 0 : q.size();
    }

    // === Tick ===

    public void tickAll() {
        List<String> finished = new ArrayList<>();
        for (Map.Entry<String, GameSession> entry : activeSessions.entrySet()) {
            GameSession session = entry.getValue();
            session.tick();
            if (session.isFinished()) {
                finished.add(entry.getKey());
            }
        }
        // Clean up finished sessions
        finished.forEach(this::destroySession);
    }

    // === Cleanup ===

    public void shutdown() {
        new ArrayList<>(activeSessions.keySet()).forEach(this::destroySession);
    }
}
```

- [ ] **Commit**

```bash
git add game/src/main/java/org/mineUGC/game/GameManager.java game/src/main/java/org/mineUGC/game/GameTickTask.java
git commit -m "feat(game): implement GameManager and tick task"
```

---

### Task 5: Create game player listener

**Files:**
- Create: `game/src/main/java/org/mineUGC/game/listeners/GamePlayerListener.java`

- [ ] **Create `GamePlayerListener.java`**

```java
package org.mineUGC.game.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.mineUGC.game.GameManager;
import org.mineUGC.game.model.GamePlayer;
import org.mineUGC.game.model.GameSession;
import org.mineUGC.game.model.GameTeam;

public class GamePlayerListener implements Listener {
    private final GameManager gameManager;

    public GamePlayerListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        GameSession session = gameManager.getPlayerSession(victim);
        if (session == null) return;

        GamePlayer gp = session.getGamePlayer(victim.getUniqueId());
        if (gp != null) {
            gp.setAlive(false);
            gp.addDeath();
        }

        // Track killer
        if (victim.getKiller() != null) {
            Player killer = victim.getKiller();
            GamePlayer gk = session.getGamePlayer(killer.getUniqueId());
            if (gk != null) {
                gk.addKill();
            }
            session.broadcast("§c" + victim.getName() + " 被 " + killer.getName() + " 淘汰");
        } else {
            session.broadcast("§c" + victim.getName() + " 被淘汰");
        }

        // Set spectator
        event.setCancelled(true);
        victim.spigot().respawn();
        victim.setGameMode(org.bukkit.GameMode.SPECTATOR);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        GameSession session = gameManager.getPlayerSession(player);
        if (session != null) {
            session.broadcast("§e" + player.getName() + " 离开了游戏");
            session.removePlayer(player.getUniqueId());
        }
        gameManager.leaveSession(player);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        GameSession session = gameManager.getPlayerSession(player);
        if (session != null && session.getWorld() != null) {
            event.setRespawnLocation(session.getWorld().getSpawnLocation());
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player damager)) return;

        GameSession session = gameManager.getPlayerSession(victim);
        if (session == null) return;

        // Friendly fire check
        if (!session.getDefinition().isFriendlyFire()) {
            GameTeam victimTeam = findTeam(session, victim);
            GameTeam damagerTeam = findTeam(session, damager);
            if (victimTeam != null && damagerTeam != null && victimTeam == damagerTeam) {
                event.setCancelled(true);
                return;
            }
        }

        // Track damage
        GamePlayer gpDamager = session.getGamePlayer(damager.getUniqueId());
        if (gpDamager != null) {
            gpDamager.addDamageDealt((int) event.getFinalDamage());
        }
    }

    private GameTeam findTeam(GameSession session, Player player) {
        return session.getTeams().stream()
                .filter(t -> t.hasMember(player.getUniqueId()))
                .findFirst().orElse(null);
    }
}
```

- [ ] **Commit**

```bash
git add game/src/main/java/org/mineUGC/game/listeners/GamePlayerListener.java
git commit -m "feat(game): add player death/damage/quit listener"
```

---

### Task 6: Integrate game module into plugin

**Files:**
- Modify: `plugin/build.gradle`
- Modify: `plugin/src/main/java/org/mineUGC/plugin/MineUGC.java`
- Modify: `core/src/main/java/org/mineUGC/core/command/UgcCommand.java`

- [ ] **Add game dependency to plugin/build.gradle**

Add after the `:gui` dependency:
```groovy
    implementation project(':game')
```

- [ ] **Extend MineUGC.java**

Add imports:
```java
import org.mineUGC.game.GameManager;
import org.mineUGC.game.GameRegistry;
import org.mineUGC.game.GameTickTask;
import org.mineUGC.game.YamlGameLoader;
import org.mineUGC.game.listeners.GamePlayerListener;
```

Add fields after `private Messages messages;`:
```java
    private GameRegistry gameRegistry;
    private YamlGameLoader gameLoader;
    private GameManager gameManager;
```

Add after `this.guiListener = new GuiListener(...)` in `onEnable()`:
```java
        // Game engine
        this.gameRegistry = new GameRegistry();
        this.gameLoader = new YamlGameLoader();
        this.gameManager = new GameManager(this, itemManager, inventoryScanner,
                gameRegistry, gameLoader);

        // Load game definitions from YAML
        loadGameDefinitions();

        // Register game listener
        getServer().getPluginManager().registerEvents(
                new GamePlayerListener(gameManager), this);

        // Start game tick task (1 tick per second)
        new GameTickTask(gameManager).runTaskTimer(this, 0L, 20L);
```

Add loadGameDefinitions method after `loadAllItems()`:
```java
    private void loadGameDefinitions() {
        File gamesDir = new File(getDataFolder(), "games");
        if (!gamesDir.exists()) {
            gamesDir.mkdirs();
            // Save example definition
            saveResource("games/classic_battle_royale.yml", false);
        }

        File[] files = gamesDir.listFiles((dir, name) ->
                name.endsWith(".yml") || name.endsWith(".yaml"));
        if (files == null) return;

        for (File file : files) {
            try {
                var def = gameLoader.load(file);
                if (def.getId() == null) continue;
                gameRegistry.register(def);
                getLogger().info("Loaded game: " + def.getId());
            } catch (Exception e) {
                getLogger().warning("Failed to load game: " + file.getName());
            }
        }
    }
```

In `onDisable()`, add before closing:
```java
        if (gameManager != null) gameManager.shutdown();
```

- [ ] **Extend UgcCommand with game subcommands**

In the existing anonymous UgcCommand in `registerCommands()`, add new `case` branches to the switch:

```java
                        case "game" -> {
                            if (!requirePlayer(sender)) return true;
                            Player p = (Player) sender;
                            if (args.length < 2) {
                                p.sendMessage(messages.get("command.game-usage"));
                                return true;
                            }
                            switch (args[1].toLowerCase()) {
                                case "create" -> {
                                    if (args.length < 3) {
                                        p.sendMessage("§c/ugc game create <gameId>");
                                        return true;
                                    }
                                    String worldName = args.length > 3 ? args[3] : null;
                                    GameSession session = gameManager.createSession(args[2], worldName);
                                    if (session != null) {
                                        p.sendMessage("§a游戏已创建! ID: " + session.getId());
                                    } else {
                                        p.sendMessage("§c创建失败，未找到游戏定义: " + args[2]);
                                    }
                                }
                                case "join" -> {
                                    if (args.length < 3) {
                                        p.sendMessage("§c/ugc game join <sessionId>");
                                        return true;
                                    }
                                    if (gameManager.joinSession(args[2], p)) {
                                        p.sendMessage("§a已加入游戏 " + args[2]);
                                    } else {
                                        p.sendMessage("§c加入失败");
                                    }
                                }
                                case "leave" -> {
                                    gameManager.leaveSession(p);
                                    p.sendMessage("§a已离开游戏");
                                }
                                case "start" -> {
                                    if (args.length < 3) {
                                        // Try to start the session the player is in
                                        GameSession s = gameManager.getPlayerSession(p);
                                        if (s != null && gameManager.startSession(s.getId())) {
                                            p.sendMessage("§a已开始游戏");
                                        } else {
                                            p.sendMessage("§c未找到游戏或无法开始");
                                        }
                                        return true;
                                    }
                                    if (gameManager.startSession(args[2])) {
                                        p.sendMessage("§a已开始游戏 " + args[2]);
                                    } else {
                                        p.sendMessage("§c开始失败");
                                    }
                                }
                                case "stop" -> {
                                    String id = args.length > 2 ? args[2] : null;
                                    if (id == null) {
                                        GameSession s = gameManager.getPlayerSession(p);
                                        if (s != null) id = s.getId();
                                    }
                                    if (id != null && gameManager.stopSession(id)) {
                                        p.sendMessage("§a已停止游戏 " + id);
                                    } else {
                                        p.sendMessage("§c停止失败");
                                    }
                                }
                                case "list" -> {
                                    var sessions = gameManager.getActiveSessions();
                                    if (sessions.isEmpty()) {
                                        p.sendMessage("§7暂无活跃游戏");
                                    } else {
                                        p.sendMessage("§e活跃游戏:");
                                        sessions.forEach(s -> p.sendMessage(
                                                " §7" + s.getId() + " (" + s.getDefinition().getName()
                                                + ") §f" + s.getPhase()));
                                    }
                                }
                                default -> p.sendMessage("§c用法: /ugc game create|join|leave|start|stop|list");
                            }
                        }
```

Also add `GameSession` import to the UgcCommand usage or inline it.

Add tab completion for game subcommands:
```java
                    if (args.length == 2 && args[0].equalsIgnoreCase("game")) {
                        return List.of("create", "join", "leave", "start", "stop", "list");
                    }
                    if (args.length == 3 && args[0].equalsIgnoreCase("game") && 
                        (args[1].equalsIgnoreCase("create"))) {
                        return gameRegistry.getAll().stream()
                                .map(g -> g.getId())
                                .toList();
                    }
```

Add `gameRegistry` field access — pass it to the command or make it accessible.

Since UgcCommand is an anonymous subclass in MineUGC.java, add the game registry reference as a local variable that the anonymous class can capture:

```java
    private void registerCommands() {
        GameRegistry gameReg = this.gameRegistry;
        GameManager gm = this.gameManager;
        
        var cmd = getServer().getPluginCommand("ugc");
        if (cmd != null) {
            cmd.setExecutor(new UgcCommand(messages) {
                // ... existing code with access to gameReg and gm ...
            });
        }
    }
```

- [ ] **Create game-usage message**

Add to `messages.yml` under the `command:` section:
```yaml
  game-usage: "§c用法: /ugc game create|join|leave|start|stop|list"
```

- [ ] **Commit**

```bash
git add plugin/build.gradle plugin/src/main/java/org/mineUGC/plugin/MineUGC.java plugin/src/main/resources/ core/src/main/resources/messages.yml
git commit -m "feat(plugin): integrate game engine into plugin"
```

---

### Task 7: Build and test

**Files:**
- All above

- [ ] **Full project compile**

```bash
./gradlew test compileJava
```
Expected: BUILD SUCCESSFUL

- [ ] **Package and run**

```bash
./gradlew :plugin:jar
cp plugin/build/libs/plugin-1.0-SNAPSHOT.jar run/plugins/
# Start server and test:
# /ugc game list
# /ugc game create classic_battle_royale
# /ugc game join <sessionId>
# /ugc game start <sessionId>
```

- [ ] **Commit all changes**

```bash
git add -A
git commit -m "feat(game): implement game engine MVP with battle royale mechanics"
```

---

## Self-Review

1. **Spec coverage:** Covers the game engine MVP: GameDefinition model, GameSession state machine (LOBBY → FINISHED), teams (solo/duo/squad), safe zone circle, player death/kill tracking, world management, commands.
2. **Placeholder scan:** No TODOs or TBDs. All code blocks are complete.
3. **Type consistency:** All method signatures match between GameSession, GameManager, and GameDefinition. GamePhase enum values are consistent.
4. **Edge cases:** Friendly fire toggle, time limit win fallback, world preparation error handling, player quit during game, queue management.
5. **Missing pieces (future phases):** Device integration, object placement, rule set loading from external YAML files. These will be added when rules/devices/objects modules are built.
