package org.drombler.media.photo.core;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class PhotoCenterErrorResponse {
    private final PhotoCenterErrorCode errorCode;
    private final String errorMessage;
}
