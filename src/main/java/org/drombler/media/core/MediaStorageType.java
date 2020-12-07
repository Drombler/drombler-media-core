package org.drombler.media.core;

import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
public enum MediaStorageType {
    SHARED_EVENTS(""),
    PRIVATE_EVENTS("P-"),
    OTHER_EVENTS("A-"), // Andere
    BUSINESS_EVENTS("G-"),  // Geschäft
    THINGS("X-"); // Photos von Dingen

    private static final Map<String, MediaStorageType> MEDIA_STORAGE_TYPE_MAP = Arrays.stream(values())
            .collect(Collectors.toMap(MediaStorageType::getPrefix, Function.identity()));

    private String prefix;

    MediaStorageType(String prefix) {
        this.prefix = prefix;
    }

    public static boolean isKnownPrefix(String prefix) {
        return MEDIA_STORAGE_TYPE_MAP.containsKey(prefix);
    }

    public static MediaStorageType getByPrefix(String prefix) {
        if (isKnownPrefix(prefix)) {
            return MEDIA_STORAGE_TYPE_MAP.get(prefix);
        } else {
            throw new IllegalArgumentException("Unknown prefix: " + prefix);
        }
    }

}
