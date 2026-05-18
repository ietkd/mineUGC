package org.mineUGC.game.model;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class GameSession {
    private final String id;
    private final GameDefinition definition;
    private final Plugin plugin;

    private GamePhase phase = GamePhase.LOBBY;
    private int phaseTicks;
    private World gameWorld;
    private boolean worldPrepared;

    // Players and teams
    private final Map<UUID, GamePlayer> players = new ConcurrentHashMap<>();
    private final List<GameTeam> teams = new CopyOnWriteArrayList<>();

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

    public GameSession(String id, GameDefinition definition, Plugin plugin) {
        this.id = id;
        this.definition = definition;
        this.plugin = plugin;
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
        teams.removeIf(t -> {
            t.removeMember(playerId);
            return t.getMemberCount() == 0;
        });
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
        currentRadius = currentRadius + (targetRadius - currentRadius) * progress;

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
            } else if (players.size() == 1) {
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
