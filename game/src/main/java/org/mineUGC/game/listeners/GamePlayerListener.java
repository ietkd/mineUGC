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
