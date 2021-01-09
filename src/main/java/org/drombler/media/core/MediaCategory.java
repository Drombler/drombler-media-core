package org.drombler.media.core;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.drombler.media.core.protocol.json.MediaCategoryType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Florian
 */
// not used yet
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public final class MediaCategory {

    @ToString.Include
    @EqualsAndHashCode.Include
    private final MediaCategoryType type;

    private final List<MediaCategoryVariant> variants;

    public MediaCategory(MediaCategoryType type, List<MediaCategoryVariant> variants) {
        this.type = type;
        this.variants = Collections.unmodifiableList(new ArrayList<>(variants));
    }

}
