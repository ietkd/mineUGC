package org.mineUGC.storage.sqlite;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseManagerTest {

    private DatabaseManager dbManager;
    private PlayerDataDAO dao;

    private void setup(File dbFile) throws SQLException {
        dbManager = new DatabaseManager(dbFile, Logger.getLogger("TestLogger"));
        dbManager.initialize();
        dao = new PlayerDataDAO(dbManager);
    }

    @AfterEach
    void tearDown() {
        if (dbManager != null) {
            dbManager.close();
        }
    }

    @Test
    void testCooldownStoresAndRetrieves(@TempDir Path tempDir) throws SQLException {
        File dbFile = tempDir.resolve("test.db").toFile();
        setup(dbFile);

        UUID playerId = UUID.randomUUID();
        long expiresAt = System.currentTimeMillis() + 60000;

        dao.setCooldown(playerId, "sword", "fireball", expiresAt);
        long retrieved = dao.getCooldownExpiry(playerId, "sword", "fireball");

        assertEquals(expiresAt, retrieved);
    }

    @Test
    void testCooldownReturnsZeroForMissing(@TempDir Path tempDir) throws SQLException {
        File dbFile = tempDir.resolve("test_missing.db").toFile();
        setup(dbFile);

        UUID playerId = UUID.randomUUID();
        long retrieved = dao.getCooldownExpiry(playerId, "nonexistent", "ability");

        assertEquals(0, retrieved);
    }

    @Test
    void testCooldownUpdatesExistingEntry(@TempDir Path tempDir) throws SQLException {
        File dbFile = tempDir.resolve("test_update.db").toFile();
        setup(dbFile);

        UUID playerId = UUID.randomUUID();
        long oldExpiry = 1000L;
        long newExpiry = 2000L;

        dao.setCooldown(playerId, "sword", "fireball", oldExpiry);
        dao.setCooldown(playerId, "sword", "fireball", newExpiry);
        long retrieved = dao.getCooldownExpiry(playerId, "sword", "fireball");

        assertEquals(newExpiry, retrieved);
    }

    @Test
    void testUnlockedItemsDefaultsToEmptyArray(@TempDir Path tempDir) throws SQLException {
        File dbFile = tempDir.resolve("test_defaults.db").toFile();
        setup(dbFile);

        UUID playerId = UUID.randomUUID();
        String items = dao.getUnlockedItems(playerId);

        assertEquals("[]", items);
    }

    @Test
    void testUnlockedItemsStoresAndRetrieves(@TempDir Path tempDir) throws SQLException {
        File dbFile = tempDir.resolve("test_items.db").toFile();
        setup(dbFile);

        UUID playerId = UUID.randomUUID();
        String itemsJson = "[\"sword\", \"shield\"]";

        dao.setUnlockedItems(playerId, itemsJson);
        String retrieved = dao.getUnlockedItems(playerId);

        assertEquals(itemsJson, retrieved);
    }

    @Test
    void testInitializeCreatesTables(@TempDir Path tempDir) throws SQLException {
        File dbFile = tempDir.resolve("test_init.db").toFile();
        setup(dbFile);

        // Verify tables exist by performing operations
        UUID playerId = UUID.randomUUID();
        dao.setCooldown(playerId, "item", "ability", 123L);
        assertEquals(123L, dao.getCooldownExpiry(playerId, "item", "ability"));

        dao.setUnlockedItems(playerId, "[\"test\"]");
        assertEquals("[\"test\"]", dao.getUnlockedItems(playerId));
    }
}
