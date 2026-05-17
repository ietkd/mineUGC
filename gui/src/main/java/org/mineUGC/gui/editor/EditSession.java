package org.mineUGC.gui.editor;

import org.bukkit.entity.Player;
import org.mineUGC.core.model.ItemDefinition;

import java.util.function.BiConsumer;

class EditSession {
    private final ItemDefinition definition;
    private final boolean existing;
    private final String originalId;
    private String pendingField;
    private String editingAbilityKey;
    private String editingPassiveKey;
    private BiConsumer<Player, GuiListener> reopenAction;

    EditSession(ItemDefinition definition, boolean existing) {
        this.definition = definition;
        this.existing = existing;
        this.originalId = definition.getId();
    }

    ItemDefinition getDefinition() { return definition; }
    boolean isExisting() { return existing; }
    String getOriginalId() { return originalId; }
    String getPendingField() { return pendingField; }
    void setPendingField(String pendingField) { this.pendingField = pendingField; }

    String getEditingAbilityKey() { return editingAbilityKey; }
    void setEditingAbilityKey(String key) { this.editingAbilityKey = key; }
    String getEditingPassiveKey() { return editingPassiveKey; }
    void setEditingPassiveKey(String key) { this.editingPassiveKey = key; }

    BiConsumer<Player, GuiListener> getReopenAction() { return reopenAction; }
    void setReopenAction(BiConsumer<Player, GuiListener> action) { this.reopenAction = action; }
}
