package org.mineUGC.storage.sqlite;

import java.sql.*;
import java.util.UUID;

public class PlayerDataDAO {
    private final DatabaseManager db;

    public PlayerDataDAO(DatabaseManager db) {
        this.db = db;
    }

    public long getCooldownExpiry(UUID playerId, String itemId, String abilityKey) throws SQLException {
        String sql = "SELECT expires_at FROM player_cooldowns WHERE player_uuid = ? AND item_id = ? AND ability_key = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, playerId.toString());
            ps.setString(2, itemId);
            ps.setString(3, abilityKey);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getLong("expires_at");
            }
            return 0;
        }
    }

    public void setCooldown(UUID playerId, String itemId, String abilityKey, long expiresAt) throws SQLException {
        String sql = "INSERT OR REPLACE INTO player_cooldowns (player_uuid, item_id, ability_key, expires_at) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, playerId.toString());
            ps.setString(2, itemId);
            ps.setString(3, abilityKey);
            ps.setLong(4, expiresAt);
            ps.executeUpdate();
        }
    }

    public String getUnlockedItems(UUID playerId) throws SQLException {
        String sql = "SELECT unlocked_items FROM player_data WHERE player_uuid = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, playerId.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("unlocked_items");
            }
            return "[]";
        }
    }

    public void setUnlockedItems(UUID playerId, String itemsJson) throws SQLException {
        String sql = "INSERT OR REPLACE INTO player_data (player_uuid, unlocked_items) VALUES (?, ?)";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, playerId.toString());
            ps.setString(2, itemsJson);
            ps.executeUpdate();
        }
    }
}
