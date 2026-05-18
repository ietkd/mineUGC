package org.mineUGC.game.model;

import java.util.*;

public class GameResult {
    private final String gameDefinitionId;
    private final GameTeam winner;
    private final Map<UUID, GamePlayer> players;
    private final int totalKills;
    private final long durationSeconds;

    public GameResult(String gameDefinitionId, GameTeam winner,
                      Map<UUID, GamePlayer> players,
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
