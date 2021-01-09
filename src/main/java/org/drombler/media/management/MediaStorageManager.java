package org.drombler.media.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.drombler.identity.core.DromblerId;
import org.drombler.identity.core.DromblerUserId;
import org.drombler.media.core.MediaCategory;
import org.drombler.media.core.MediaCategoryManager;
import org.drombler.media.core.MediaStorage;
import org.drombler.media.core.MediaStorageContentType;
import org.drombler.media.core.protocol.json.MediaCategoryType;
import org.drombler.media.management.config.model.json.MediaStorageConfig;
import org.drombler.media.management.config.model.json.MediaStorageConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toSet;

/**
 *
 * @author Florian
 */
public class MediaStorageManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(MediaStorageManager.class);

    private final MediaCategoryManager mediaCategoryManager;
    private final List<MediaStorage> mediaStorages = new ArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MediaStorageManager(MediaCategoryManager mediaCategoryManager) {
        this.mediaCategoryManager = mediaCategoryManager;
    }

    public void loadJsonConfig(InputStream is) throws IOException {
        synchronized (mediaStorages) {
            MediaStorageConfig mediaStorageConfig = objectMapper.readValue(is, MediaStorageConfig.class);
            mediaStorageConfig.getMediaStorageConfigurations().stream()
                    .map(this::createMediaStorage)
                    .forEach(this::addMediaStorage);
        }
    }

    public void storeJsonConfig(OutputStream os) throws IOException {
        synchronized (mediaStorages) {
            List<MediaStorageConfiguration> mediaStorageConfigurations = mediaStorages.stream()
                    .map(this::extractMediaStorageConfiguration)
                    .collect(Collectors.toList());

            MediaStorageConfig mediaStorageConfig = new MediaStorageConfig();
            mediaStorageConfig.setMediaStorageConfigurations(mediaStorageConfigurations);
            objectMapper.writeValue(os, mediaStorageConfig);
        }

    }

    private MediaStorage createMediaStorage(MediaStorageConfiguration configuration) {
        List<MediaCategory> supportedMediaCategories = getSupportedMediaCategories(configuration);
        Set<MediaStorageContentType> supportedContentTypes = configuration.getSupportedContentTypes().stream().map(MediaStorageContentType::valueOf).collect(toCollection(() -> EnumSet.noneOf(MediaStorageContentType.class)));
        Set<DromblerId> owners = configuration.getOwners().stream().map(DromblerUserId::new).collect(toSet());
        return new MediaStorage(StringUtils.isNotBlank(configuration.getId()) ? UUID.fromString(configuration.getId()) : null, configuration.getName(), Paths.get(configuration.getMediaRootDir()),
                supportedContentTypes, configuration.getLegacyEventDirNames(), supportedMediaCategories, owners);
    }

    private List<MediaCategory> getSupportedMediaCategories(MediaStorageConfiguration configuration) {
        return configuration.getSupportedMediaCategoryTypes().stream()
                .filter(mediaCategoryType -> {
                    if (mediaCategoryManager.containsMediaCategory(mediaCategoryType)) {
                        return true;
                    } else {
                        LOGGER.error("Unknown mediaCategoryId: {}", mediaCategoryType);
                        return false;
                    }
                })
                .map(mediaCategoryManager::getMediaCategory)
                .collect(Collectors.toList());
    }

    private MediaStorageConfiguration extractMediaStorageConfiguration(MediaStorage mediaStorage) {
        MediaStorageConfiguration mediaStorageConfiguration = new MediaStorageConfiguration();
        mediaStorageConfiguration.setName(mediaStorage.getName());
        mediaStorageConfiguration.setMediaRootDir(mediaStorage.getMediaRootDir().toString());
        mediaStorageConfiguration.setSupportedMediaCategoryTypes(getSupportedMediaCategoryTypes(mediaStorage));
        // TODO: setPrivate
//        mediaStorageConfiguration.setPrivate();
        return mediaStorageConfiguration;
    }

    private static List<MediaCategoryType> getSupportedMediaCategoryTypes(MediaStorage mediaStorage) {
        return mediaStorage.getSupportedMediaCategories().stream()
                .map(MediaCategory::getType)
                .collect(Collectors.toList());
    }

    public boolean addMediaStorage(MediaStorage mediaStorage) {
        synchronized (mediaStorages) {
            return mediaStorages.add(mediaStorage);
        }
    }

    public boolean removeMediaStorage(MediaStorage mediaStorage) {
        synchronized (mediaStorages) {
            return mediaStorages.remove(mediaStorage);
        }
    }

    public List<MediaStorage> getMediaStorages() {
        synchronized (mediaStorages) {
            return new ArrayList<>(mediaStorages);
        }
    }
}
