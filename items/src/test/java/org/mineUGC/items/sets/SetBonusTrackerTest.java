package org.mineUGC.items.sets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SetBonusTrackerTest {
    private SetBonusTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new SetBonusTracker(null);
    }

    @Test
    void getSetCounts_shouldHandleNullPlayer() {
        assertDoesNotThrow(() -> tracker.getSetCounts(null));
    }
}
