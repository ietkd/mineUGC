package org.mineUGC.core.event;

import org.bukkit.Bukkit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

class AssetReloadEventTest {

    private static MockedStatic<Bukkit> bukkitMock;

    @BeforeAll
    static void setUp() {
        bukkitMock = Mockito.mockStatic(Bukkit.class);
        bukkitMock.when(Bukkit::isPrimaryThread).thenReturn(true);
    }

    @AfterAll
    static void tearDown() {
        bukkitMock.close();
    }

    @Test
    void constructor_shouldStoreFields() {
        AssetReloadEvent event = new AssetReloadEvent("test_sword", "item");
        assertEquals("test_sword", event.getAssetId());
        assertEquals("item", event.getAssetType());
    }

    @Test
    void handlerList_shouldNotBeNull() {
        assertNotNull(AssetReloadEvent.getHandlerList());
    }

    @Test
    void event_shouldHaveCorrectHandlers() {
        AssetReloadEvent event = new AssetReloadEvent("a", "item");
        assertSame(AssetReloadEvent.getHandlerList(), event.getHandlers());
    }
}
