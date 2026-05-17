package org.mineUGC.plugin;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class MineUGCTest {

    private static ServerMock server;

    @BeforeAll
    static void setUp() {
        server = MockBukkit.mock();
    }

    @AfterAll
    static void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void pluginClass_shouldBeLoadable() {
        MineUGC plugin = MockBukkit.load(MineUGC.class);
        assertNotNull(plugin);
    }
}
