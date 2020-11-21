package org.drombler.media.core;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

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
    private final String id;

    private final List<MediaCategoryVariant> variants;

    public MediaCategory(String id, List<MediaCategoryVariant> variants) {
        this.id = id;
        this.variants = Collections.unmodifiableList(new ArrayList<>(variants));
    }

}
