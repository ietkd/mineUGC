package org.mineUGC.core.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class AssetReloadEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final String assetId;
    private final String assetType;

    public AssetReloadEvent(String assetId, String assetType) {
        super(!org.bukkit.Bukkit.isPrimaryThread());
        this.assetId = assetId;
        this.assetType = assetType;
    }

    public String getAssetId() { return assetId; }
    public String getAssetType() { return assetType; }

    public static HandlerList getHandlerList() { return handlers; }

    @Override
    public @NotNull HandlerList getHandlers() { return handlers; }
}
