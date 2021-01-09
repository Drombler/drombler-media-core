package org.drombler.media.image.core;

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
public class ImageMediaCategoryProvider implements MediaCategoryProvider {

    private final MediaCategory imageMediaCategory = new MediaCategory(MediaCategoryType.IMAGE, Arrays.asList(
            new MediaCategoryVariant(new HashSet<>(asList("image/jpeg")), new HashSet<>(Arrays.asList(".jpeg", ".jpg"))),
            new MediaCategoryVariant(new HashSet<>(asList("image/png")), new HashSet<>(Arrays.asList(".png"))),
            new MediaCategoryVariant(new HashSet<>(asList("image/gif")), new HashSet<>(Arrays.asList(".gif")))
    ));

    @Override
    public MediaCategory getMediaCategory() {
        return imageMediaCategory;
    }

}
