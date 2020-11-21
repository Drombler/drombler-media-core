package org.drombler.media.core;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.drombler.event.core.Event;
import org.drombler.identity.core.DromblerId;
import org.softsmithy.lib.text.FormatException;

import java.nio.file.Path;

/**
 * @author Florian
 */
@Getter
@Setter
@RequiredArgsConstructor
public class MediaSource {

    public Path getPath() throws FormatException { // TODO: avoid FormatException here?
        return getMediaStorage().resolveMediaEventDirPath(getEvent(), getCopyrightOwner(), false)
                .resolve(getFileName());
    }

    private final MediaStorage mediaStorage;
    private final Path fileName;

    private Event event;
    private DromblerId copyrightOwner;

}
