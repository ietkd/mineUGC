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
