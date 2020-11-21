package org.drombler.media.photo.core;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class MediaCenterErrorResponse {
    private final MediaCenterErrorCode errorCode;
    private final String errorMessage;
}
