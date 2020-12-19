package org.drombler.media.photo.core;

import org.drombler.media.core.MediaCategory;
import org.drombler.media.core.MediaCategoryProvider;
import org.drombler.media.core.MediaCategoryVariant;
import org.drombler.media.core.protocol.json.MediaCategoryType;

import java.util.Arrays;
import java.util.HashSet;

import static java.util.Arrays.asList;

/**
 *
 * @author Florian
 */
public class PhotoMediaCategoryProvider implements MediaCategoryProvider {

    private final MediaCategory photoMediaCategory = new MediaCategory(MediaCategoryType.PHOTO, Arrays.asList(
            new MediaCategoryVariant(new HashSet<>(asList("image/jpeg")), new HashSet<>(Arrays.asList(".jpeg", ".jpg"))),
            new MediaCategoryVariant(new HashSet<>(asList("image/png")), new HashSet<>(Arrays.asList(".png")))
    ));

    @Override
    public MediaCategory getMediaCategory() {
        return photoMediaCategory;
    }

}
