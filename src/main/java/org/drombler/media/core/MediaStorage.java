package org.drombler.media.core;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
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
import java.util.*;
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
    private final MediaStorageType type;
    private final boolean legacyEventDirNames;
    private final List<MediaCategory> supportedMediaCategories;

    public MediaStorage(String id, String name, Path mediaRootDir, MediaStorageType type, boolean legacyEventDirNames, List<MediaCategory> supportedMediaCategories) {
        this.id = id;
        this.name = name;
        this.mediaRootDir = mediaRootDir;
        this.type = type;
        this.legacyEventDirNames = legacyEventDirNames;
        this.supportedMediaCategories = Collections.unmodifiableList(new ArrayList<>(supportedMediaCategories));
        if (!Files.exists(mediaRootDir) || !Files.isDirectory(mediaRootDir)) {
            throw new IllegalArgumentException("Not a valid directory: " + mediaRootDir);
        }
    }

    private Path resolveMediaEventDirPath(Event event, boolean uncategorized) throws FormatException {
        Path mediaRootDirPath = getMediaRootDirPath(uncategorized);
        return resolveMediaEventDirPath(mediaRootDirPath, event);
    }

    private Path resolveMediaEventDirPath(Path mediaRootDirPath, Event event) throws FormatException {
        StringBuilder sb = formatEventDirName(event);
        return mediaRootDirPath.resolve(sb.toString());
    }

    private StringBuilder formatEventDirName(Event event) throws FormatException {
        EventDirNameFormatter formatter = new EventDirNameFormatter();
        StringBuilder sb = new StringBuilder(type.getPrefix());
        formatter.format(event, sb);
        return sb;
    }

    /* package-private */ Path resolveMediaEventDirPath(Event event, DromblerId copyrightOwner, boolean uncategorized) throws FormatException {
        return resolveMediaEventDirPath(event, uncategorized).resolve(copyrightOwner.getDromblerIdFormatted());
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

    public List<Event> parseEvents() throws IOException {
        List<Event> events = new ArrayList<>();
        try (DirectoryStream<Path> eventDirPathStream = Files.newDirectoryStream(getMediaRootDir())) {
            for (Path eventDirPath : eventDirPathStream) {
                parseEvent(eventDirPath)
                        .ifPresent(events::add);
            }
        }
        return events;
    }

    private Optional<Event> parseEvent(Path eventDirPath) {
        if (Files.isDirectory(eventDirPath)) {
            Matcher matcher = DIR_PATTERN.matcher(eventDirPath.getFileName().toString());
            if (matcher.find()) {
                Optional<MediaStorageType> parsedType = parseType(matcher);
                String eventDirName = matcher.group(2);
                if (parsedType.isPresent()
                        && (type.equals(parsedType.get()) ||
                        (parsedType.get().equals(MediaStorageType.SHARED_EVENTS) && legacyEventDirNames))) {
                    try {
                        return Optional.of(parseEvent(eventDirName));
                    } catch (ParseException ex) {
                        LOGGER.error("Could not parse: " + eventDirPath, ex);
                    }
                } else {
                    if (!parsedType.isPresent()) {
                        LOGGER.error("Unknown event dir prefix in event dir name: {}", eventDirName);
                    }
                }
            } else {
                LOGGER.warn("Event directory expected: {}", eventDirPath);
            }
        }
        return Optional.empty();
    }

    private Optional<MediaStorageType> parseType(Matcher matcher) {
        String prefix = StringUtils.stripToEmpty(matcher.group(1));
        if (MediaStorageType.isKnownPrefix(prefix)) {
            return Optional.of(MediaStorageType.getByPrefix(prefix));
        } else {
            return Optional.empty();
        }
    }


    public Map<DromblerId, List<MediaSource>> loadMediaSources(Path eventDirPath, Event event, DromblerIdentityProviderManager dromblerIdentityProviderManager) throws IOException {
        try (DirectoryStream<Path> eventCopyrightOwnerDirPathStream = Files.newDirectoryStream(eventDirPath)) {
            boolean noCopyrightOwnerWarningLogged = false;
            Map<DromblerId, List<MediaSource>> mediaSourceMap = new HashMap<>();
            for (Path eventCopyrightOwnerDirPath : eventCopyrightOwnerDirPathStream) {
                if (Files.isDirectory(eventCopyrightOwnerDirPath)) {
                    loadMediaSources(mediaSourceMap, event, eventCopyrightOwnerDirPath, dromblerIdentityProviderManager);
                } else {
                    if (!noCopyrightOwnerWarningLogged) {
                        LOGGER.warn("Some media source have no assigned copyright owner in eventDirPath: {}", eventDirPath);
                        // log only once
                        noCopyrightOwnerWarningLogged = true;
                    }
                    Path mediaSourcePath = eventCopyrightOwnerDirPath;
                    MediaSource mediaSource = createMediaSource(mediaSourcePath.getFileName(), event, null);
                    mediaSourceMap.getOrDefault(null, new ArrayList<>()).add(mediaSource);
                }
            }
            return mediaSourceMap;
        }
    }

    public void loadMediaSources(Map<DromblerId, List<MediaSource>> mediaSourceMap, Event event, Path eventCopyrightOwnerDirPath, DromblerIdentityProviderManager dromblerIdentityProviderManager) throws IOException {
        DromblerUserId copyrightOwner = parseCopyrightOwner(eventCopyrightOwnerDirPath.getFileName().toString(), dromblerIdentityProviderManager);
        try (DirectoryStream<Path> mediaSourcePathStream = Files.newDirectoryStream(eventCopyrightOwnerDirPath)) {
            List<MediaSource> mediaSources = new ArrayList<>();
            for (Path mediaSourcePath : mediaSourcePathStream) {
                MediaSource mediaSource = createMediaSource(mediaSourcePath.getFileName(), event, copyrightOwner);
                mediaSources.add(mediaSource);
            }
            mediaSourceMap.put(copyrightOwner, mediaSources);
        }
    }

    private MediaSource createMediaSource(final Path mediaFileName, Event event, DromblerId copyrightOwner) {
        MediaSource mediaSource = createMediaSource(mediaFileName);
        mediaSource.setEvent(event);
        mediaSource.setCopyrightOwner(copyrightOwner);
        return mediaSource;
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

    public void mergeEventDirs(Event sourceEvent, Event targetEvent) throws FormatException, IOException {
        Path sourceMediaEventDirPath = resolveMediaEventDirPath(sourceEvent, false);
        Path targetMediaEventDirPath = resolveMediaEventDirPath(targetEvent, false);
        createMediaEventDir(targetMediaEventDirPath);
        CopyFileVisitor.move(sourceMediaEventDirPath, targetMediaEventDirPath);
    }

    public Path importFile(Path filePath, Event event, DromblerId copyrightOwner, FileMigrationOperation fileMigrationOperation) throws IOException, FormatException {
        return importFile(filePath, event, copyrightOwner, false, fileMigrationOperation);
    }

    public Path importFile(Path filePath, Event event, DromblerId copyrightOwner, boolean uncategorized, FileMigrationOperation fileMigrationOperation) throws IOException, FormatException {
        Path mediaEventDirPath = resolveMediaEventDirPath(event, copyrightOwner, uncategorized);
        createMediaEventDir(mediaEventDirPath);
        Path targetFile = mediaEventDirPath.resolve(filePath.getFileName());
        fileMigrationOperation.migrate(filePath, targetFile);
        return targetFile;
    }

    private void createMediaEventDir(Path mediaEventDirPath) throws IOException {
        if (!Files.exists(mediaEventDirPath)) {
            Files.createDirectories(mediaEventDirPath);
        }
    }
}
