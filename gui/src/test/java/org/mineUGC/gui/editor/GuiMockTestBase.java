package org.mineUGC.gui.editor;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mineUGC.core.message.Messages;
import org.mineUGC.items.ItemManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

/**
 * Base class for GUI tests that need MockBukkit.
 * Ensures proper MockBukkit lifecycle and common mocks.
 */
public abstract class GuiMockTestBase {

    protected static ServerMock server;
    protected static Plugin plugin;
    protected static ItemManager itemManager;
    protected GuiListener guiListener;
    protected File itemsDir;

    @BeforeAll
    static void globalSetUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin("mineUGC");
        itemManager = new ItemManager(plugin);
    }

    @AfterAll
    static void globalTearDown() {
        MockBukkit.unmock();
    }

    @BeforeEach
    void baseSetUp(@TempDir File tempDir) {
        itemsDir = new File(tempDir, "items");
        itemsDir.mkdirs();
        guiListener = new GuiListener(plugin, itemManager, itemsDir, new Messages());
    }
}
