package org.drombler.media.core;

import org.drombler.media.core.protocol.json.MediaCategoryType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MediaCategoryManagerTest {
    private final MediaCategoryManager testee = new MediaCategoryManager();

    @Test
    public void testContainsMediaCategory() {
        assertTrue(testee.containsMediaCategory(MediaCategoryType.PHOTO));
        assertTrue(testee.containsMediaCategory(MediaCategoryType.VIDEO));
        assertTrue(testee.containsMediaCategory(MediaCategoryType.IMAGE));
    }
}