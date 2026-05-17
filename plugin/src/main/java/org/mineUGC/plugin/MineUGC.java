package org.mineUGC.plugin;

import org.bukkit.plugin.java.JavaPlugin;

public final class MineUGC extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("mineUGC enabled");
    }

    @Override
    public void onDisable() {
        getLogger().info("mineUGC disabled");
    }
}
