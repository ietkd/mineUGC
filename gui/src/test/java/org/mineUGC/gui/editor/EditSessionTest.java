package org.mineUGC.gui.editor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.bukkit.entity.Player;
import org.mineUGC.core.model.ItemDefinition;

import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.*;

class EditSessionTest {

    private ItemDefinition def;
    private EditSession session;

    @BeforeEach
    void setUp() {
        def = new ItemDefinition();
        def.setId("test_item");
        def.setName("&aTest Item");
        def.setMaterial("DIAMOND_SWORD");
        session = new EditSession(def, true);
    }

    @Test
    void constructor_shouldStoreDefinitionAndExistingFlag() {
        assertSame(def, session.getDefinition());
        assertTrue(session.isExisting());
        assertEquals("test_item", session.getOriginalId());
    }

    @Test
    void constructor_shouldStoreOriginalIdBeforeModification() {
        EditSession s = new EditSession(def, false);
        assertEquals("test_item", s.getOriginalId());
        def.setId("renamed");
        assertEquals("test_item", s.getOriginalId());
    }

    @Test
    void constructor_shouldMarkNewSessionAsNotExisting() {
        ItemDefinition newDef = new ItemDefinition();
        newDef.setId("new_item");
        EditSession s = new EditSession(newDef, false);
        assertFalse(s.isExisting());
        assertEquals("new_item", s.getOriginalId());
    }

    @Test
    void constructor_shouldKeepOriginalIdWhenIdChanges() {
        EditSession s = new EditSession(def, true);
        def.setId("renamed");
        assertEquals("test_item", s.getOriginalId());
        assertEquals("renamed", def.getId());
    }

    @Test
    void pendingField_shouldRoundTrip() {
        assertNull(session.getPendingField());
        session.setPendingField("name");
        assertEquals("name", session.getPendingField());
        session.setPendingField(null);
        assertNull(session.getPendingField());
    }

    @Test
    void editingAbilityKey_shouldRoundTrip() {
        assertNull(session.getEditingAbilityKey());
        session.setEditingAbilityKey("right_click");
        assertEquals("right_click", session.getEditingAbilityKey());
        session.setEditingAbilityKey(null);
        assertNull(session.getEditingAbilityKey());
    }

    @Test
    void editingPassiveKey_shouldRoundTrip() {
        assertNull(session.getEditingPassiveKey());
        session.setEditingPassiveKey("on_hit");
        assertEquals("on_hit", session.getEditingPassiveKey());
        session.setEditingPassiveKey(null);
        assertNull(session.getEditingPassiveKey());
    }

    @Test
    void reopenAction_shouldRoundTrip() {
        assertNull(session.getReopenAction());
        BiConsumer<Player, GuiListener> action = (p, g) -> {};
        session.setReopenAction(action);
        assertSame(action, session.getReopenAction());
        session.setReopenAction(null);
        assertNull(session.getReopenAction());
    }

    @Test
    void session_shouldBeIndependentBetweenInstances() {
        ItemDefinition defA = new ItemDefinition();
        defA.setId("a");
        ItemDefinition defB = new ItemDefinition();
        defB.setId("b");

        EditSession sessionA = new EditSession(defA, false);
        EditSession sessionB = new EditSession(defB, true);

        sessionA.setPendingField("name");
        sessionB.setPendingField("material");

        assertEquals("name", sessionA.getPendingField());
        assertEquals("material", sessionB.getPendingField());
        assertFalse(sessionA.isExisting());
        assertTrue(sessionB.isExisting());
    }
}
