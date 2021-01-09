package org.drombler.media.core;

import org.drombler.media.core.protocol.json.MediaCategoryType;
import org.drombler.media.image.core.ImageMediaCategoryProvider;
import org.drombler.media.photo.core.PhotoMediaCategoryProvider;
import org.drombler.media.video.core.VideoMediaCategoryProvider;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Florian
 */
public class MediaCategoryManager {

    private final Map<MediaCategoryType, MediaCategory> mediaCategoryMap;

    public MediaCategoryManager() {
        this.mediaCategoryMap = Stream.of(new PhotoMediaCategoryProvider(), new VideoMediaCategoryProvider(), new ImageMediaCategoryProvider())
                .map(MediaCategoryProvider::getMediaCategory)
                .collect(Collectors.toMap(MediaCategory::getType, Function.identity(), this::throwIllegalStateException,
                        () -> new EnumMap<>(MediaCategoryType.class)));
    }

    private MediaCategory throwIllegalStateException(MediaCategory mediaCategory1, MediaCategory mediaCategory2) {
        throw new IllegalStateException("Duplicated key detected!");
    }

    public boolean containsMediaCategory(MediaCategoryType mediaCategoryType) {
        return mediaCategoryMap.containsKey(mediaCategoryType);
    }

    public MediaCategory getMediaCategory(MediaCategoryType mediaCategoryType) {
        return mediaCategoryMap.get(mediaCategoryType);
    }
}
