package org.mineUGC.game;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.mineUGC.game.model.GameDefinition;
import org.mineUGC.game.model.GameSession;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameManager {
    private final Plugin plugin;
    private final GameRegistry gameRegistry;

    private final Map<String, GameSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerSessions = new ConcurrentHashMap<>();

    // Queue management
    private final Map<String, List<UUID>> queues = new ConcurrentHashMap<>();

    public GameManager(Plugin plugin, GameRegistry gameRegistry) {
        this.plugin = plugin;
        this.gameRegistry = gameRegistry;
    }

    // === Session Lifecycle ===

    public GameSession createSession(String gameDefinitionId, String worldName) {
        GameDefinition def = gameRegistry.get(gameDefinitionId);
        if (def == null) return null;

        String sessionId = UUID.randomUUID().toString().substring(0, 8);
        GameSession session = new GameSession(sessionId, def, plugin);  // 3-arg constructor!

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

        // Clean up — remove each player by UUID from playerSessions
        session.getPlayers().forEach(gp ->
            playerSessions.remove(gp.getPlayerId()));
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
        queues.computeIfAbsent(gameDefinitionId, k -> Collections.synchronizedList(new ArrayList<>())).add(player.getUniqueId());
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
