package org.drombler.media.core;

import lombok.Getter;

import java.util.*;

/**
 *
 * @author Florian
 */
@Getter
public final class MediaCategoryVariant {

    private final Set<String> mimeTypes;
    private final Set<String> fileExtensions;
    private final List<MediaCategoryVariant> supplementVariants;

    public MediaCategoryVariant(Set<String> mimeTypes, Set<String> fileExtensions) {
        this(mimeTypes, fileExtensions, Collections.emptyList());
    }
    public MediaCategoryVariant(Set<String> mimeTypes, Set<String> fileExtensions, List<MediaCategoryVariant> supplementVariants) {
        this.mimeTypes = Collections.unmodifiableSet(new HashSet<>(mimeTypes));
        this.fileExtensions = Collections.unmodifiableSet(new HashSet<>(fileExtensions));
        this.supplementVariants = Collections.unmodifiableList(new ArrayList<>(supplementVariants));
    }
    
}
