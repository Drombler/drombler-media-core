package org.drombler.media.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MediaCategoryManagerTest {
    private final MediaCategoryManager testee = new MediaCategoryManager();

    @Test
    public void testContainsMediaCategory() {
        assertTrue(testee.containsMediaCategory("photo"));
        assertTrue(testee.containsMediaCategory("video"));
        assertTrue(testee.containsMediaCategory("image"));
    }
}