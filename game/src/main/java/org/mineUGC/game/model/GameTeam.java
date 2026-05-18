package org.mineUGC.game.model;

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
    public int getAliveCount() { return alive ? members.size() : 0; }
    public int getScore() { return score; }
    public void addScore(int points) { this.score += points; }
    public boolean isAlive() { return alive && !members.isEmpty(); }
    public void setAlive(boolean alive) { this.alive = alive; }
}
