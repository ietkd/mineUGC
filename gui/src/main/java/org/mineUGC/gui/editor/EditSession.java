package org.mineUGC.gui.editor;

import org.mineUGC.core.model.ItemDefinition;

class EditSession {
    private final ItemDefinition definition;
    private final boolean existing;
    private String pendingField;

    EditSession(ItemDefinition definition, boolean existing) {
        this.definition = definition;
        this.existing = existing;
    }

    ItemDefinition getDefinition() {
        return definition;
    }

    boolean isExisting() {
        return existing;
    }

    String getPendingField() {
        return pendingField;
    }

    void setPendingField(String pendingField) {
        this.pendingField = pendingField;
    }
}
