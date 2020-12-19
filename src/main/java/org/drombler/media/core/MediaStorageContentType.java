package org.drombler.media.core;

import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
public enum MediaStorageContentType {
    SHARED_EVENTS(""),
    PRIVATE_EVENTS("P-"),
    OTHER_EVENTS("A-"), // Andere
    BUSINESS("G-"),  // Geschäft
    MEDICAL("M-"),
    THINGS("X-"); // Photos von Dingen

    private static final Map<String, MediaStorageContentType> MEDIA_STORAGE_CONTENT_TYPE_MAP = Arrays.stream(values())
            .collect(Collectors.toMap(MediaStorageContentType::getPrefix, Function.identity()));

    private String prefix;

    MediaStorageContentType(String prefix) {
        this.prefix = prefix;
    }

    public static boolean isKnownPrefix(String prefix) {
        return MEDIA_STORAGE_CONTENT_TYPE_MAP.containsKey(prefix);
    }

    public static MediaStorageContentType getByPrefix(String prefix) {
        if (isKnownPrefix(prefix)) {
            return MEDIA_STORAGE_CONTENT_TYPE_MAP.get(prefix);
        } else {
            throw new IllegalArgumentException("Unknown prefix: " + prefix);
        }
    }

}
