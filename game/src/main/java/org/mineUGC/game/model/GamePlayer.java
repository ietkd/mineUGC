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
