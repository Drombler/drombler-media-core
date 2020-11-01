package org.drombler.media.core;

import lombok.Getter;
import org.drombler.event.core.Event;
import org.drombler.event.core.format.EventDirNameFormatter;
import org.drombler.event.core.format.EventDirNameParser;
import org.drombler.identity.core.DromblerId;
import org.drombler.identity.core.DromblerUserId;
import org.drombler.identity.management.DromblerIdentityProviderManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.softsmithy.lib.text.FormatException;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Getter
public class MediaStorage {

    private static final Logger LOGGER = LoggerFactory.getLogger(MediaStorage.class);

    private static final String UNCATEGORIZED_DIR_NAME = "uncategorized"; // used before imports (only?), e.g. D:/hd-writer-ae-tmp/video/uncategorized
    private static final Pattern DIR_PATTERN = Pattern.compile("^(\\w-)?+(.*)");

    private final String id;
    private final String name;
    private final Path mediaRootDir;
    private final List<MediaCategory> supportedMediaCategories;

    public MediaStorage(String id, String name, Path mediaRootDir, List<MediaCategory> supportedMediaCategories) {
        this.id = id;
        this.name = name;
        this.mediaRootDir = mediaRootDir;
        this.supportedMediaCategories = Collections.unmodifiableList(new ArrayList<>(supportedMediaCategories));
        if (!Files.exists(mediaRootDir) || !Files.isDirectory(mediaRootDir)) {
            throw new IllegalArgumentException("Not a valid directory: " + mediaRootDir);
        }
    }

    public Path resolveMediaEventDirPath(Event event, DromblerId dromblerId, boolean uncategorized) throws FormatException {
        Path mediaRootDirPath = getMediaRootDirPath(uncategorized);
        return resolveMediaEventDirPath(mediaRootDirPath, event, dromblerId);
    }

    private Path resolveMediaEventDirPath(Path mediaRootDirPath, Event event, DromblerId dromblerId) throws FormatException {
        EventDirNameFormatter formatter = new EventDirNameFormatter();
        return mediaRootDirPath.resolve(formatter.format(event)).resolve(dromblerId.getDromblerIdFormatted());
    }

    private Path getMediaRootDirPath(boolean uncategorized) {
        if (!uncategorized) {
            return getMediaRootDir();
        } else {
            return getUncategorizedMediaRootDir();
        }
    }

    public boolean isSupportedByFileExtension(String fileName) {
        int index = fileName.lastIndexOf(".");
        if (index >= 0) {
            String fileExtension = fileName.substring(index).toLowerCase();
            return supportedMediaCategories.stream()
                    .map(MediaCategory::getVariants)
                    .flatMap(Collection::stream)
                    .flatMap(variant -> Stream.concat(
                            variant.getFileExtensions().stream(),
                            variant.getSupplementVariants().stream()
                                    .map(MediaCategoryVariant::getFileExtensions) // TODO: support deep recursion? needed?
                                    .flatMap(Collection::stream)))
                    .anyMatch(supportedFileExtension -> supportedFileExtension.equals(fileExtension));
        } else {
            return false;
        }

    }

    public Path getUncategorizedMediaRootDir() {
        return getMediaRootDir().resolve(UNCATEGORIZED_DIR_NAME);
    }

    public List<MediaSource> readMediaSources(DromblerIdentityProviderManager dromblerIdentityProviderManager) throws IOException {
        List<MediaSource> mediaSources = new ArrayList<>();
        try (DirectoryStream<Path> eventDirPathStream = Files.newDirectoryStream(getMediaRootDir())) {
            for (Path eventDirPath : eventDirPathStream) {
                addMediaSources(mediaSources, eventDirPath, dromblerIdentityProviderManager);
            }
        }
        return mediaSources;
    }

    private void addMediaSources(List<MediaSource> mediaSources, Path eventDirPath, DromblerIdentityProviderManager dromblerIdentityProviderManager) throws IOException {
        if (Files.isDirectory(eventDirPath)) {
            Matcher matcher = DIR_PATTERN.matcher(eventDirPath.getFileName().toString());
            if (matcher.find()) {
                String prefix = matcher.group(1);
                String eventDirName = matcher.group(2);
                if (prefix == null
                        || prefix.equals("A-") // Andere
                        || prefix.equals("P-")) { // Privat
                    try {
                        Event event = parseEvent(eventDirName);
                        addMediaSources(eventDirPath, mediaSources, event, dromblerIdentityProviderManager);
                    } catch (ParseException ex) {
                        LOGGER.error("Could not parse: " + eventDirPath, ex);
                    }
                } else {
                    if (!prefix.equals("G-") // Geschäft
                            && !prefix.equals("X-")) { // Photos von Dingen
                        LOGGER.error("Unknown event dir prefix in event dir name: {}", eventDirName);
                    }
                }
            } else {
                LOGGER.warn("Event directory expected: {}", eventDirPath);
            }
        }
    }

    private void addMediaSources(Path eventDirPath, List<MediaSource> mediaSources, Event event, DromblerIdentityProviderManager dromblerIdentityProviderManager) throws IOException {
        try (DirectoryStream<Path> eventCopyrightOwnerDirPathStream = Files.newDirectoryStream(eventDirPath)) {
            boolean noCopyrightOwnerWarningLogged = false;
            for (Path eventCopyrightOwnerDirPath : eventCopyrightOwnerDirPathStream) {
                if (Files.isDirectory(eventCopyrightOwnerDirPath)) {
                    addMediaSources(mediaSources, event, eventCopyrightOwnerDirPath, dromblerIdentityProviderManager);
                } else {
                    if (!noCopyrightOwnerWarningLogged) {
                        LOGGER.warn("Some media source have no assigned copyright owner in eventDirPath: {}", eventDirPath);
                        // log only once
                        noCopyrightOwnerWarningLogged = true;
                    }
                    Path mediaSourcePath = eventCopyrightOwnerDirPath;
                    addMediaSource(mediaSources, event, null, mediaSourcePath);
                }
            }
        }
    }

    private void addMediaSources(List<MediaSource> mediaSources, Event event, Path eventCopyrightOwnerDirPath, DromblerIdentityProviderManager dromblerIdentityProviderManager) throws IOException {
        DromblerUserId copyrightOwner = parseCopyrightOwner(eventCopyrightOwnerDirPath.getFileName().toString(), dromblerIdentityProviderManager);
        try (DirectoryStream<Path> mediaSourcePathStream = Files.newDirectoryStream(eventCopyrightOwnerDirPath)) {
            for (Path mediaSourcePath : mediaSourcePathStream) {
                addMediaSource(mediaSources, event, copyrightOwner, mediaSourcePath);
            }
        }
    }

    private void addMediaSource(List<MediaSource> mediaSources, Event event, DromblerId copyrightOwner, Path mediaSourcePath) {
        final Path fileName = mediaSourcePath.getFileName();
        MediaSource mediaSource = createMediaSource(event, copyrightOwner, fileName);
        if (mediaSource != null) {
            mediaSources.add(mediaSource);
        }
    }

    private MediaSource createMediaSource(Event event, DromblerId copyrightOwner, final Path mediaFileName) {
        try {
            MediaSource mediaSource = createMediaSource(mediaFileName);
            mediaSource.setEvent(event);
            mediaSource.setCopyrightOwner(copyrightOwner);
            return mediaSource;
        } catch (RuntimeException ex) {
            LOGGER.error("Unexpected error for event={}, copyrightOwner={}, photoFileName={}: {}", event, copyrightOwner, mediaFileName, ex.getMessage());
            return null;
        }
    }

    private DromblerUserId parseCopyrightOwner(final String copyrightOwnerDirName, DromblerIdentityProviderManager dromblerIdentityProviderManager) {
        return copyrightOwnerDirName != null
                ? DromblerUserId.parseDromblerUserId(copyrightOwnerDirName, dromblerIdentityProviderManager)
                : null;
    }

    private Event parseEvent(final String eventDirName) throws ParseException {
        EventDirNameParser parser = new EventDirNameParser();
        return parser.parse(eventDirName);
    }

    protected MediaSource createMediaSource(final Path mediaFileName) {
        return new MediaSource(this, mediaFileName);
    }

}
