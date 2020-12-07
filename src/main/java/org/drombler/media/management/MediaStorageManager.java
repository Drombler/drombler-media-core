package org.drombler.media.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.drombler.media.core.MediaCategory;
import org.drombler.media.core.MediaCategoryManager;
import org.drombler.media.core.MediaStorage;
import org.drombler.media.core.MediaStorageType;
import org.drombler.media.management.config.model.json.MediaStorageConfig;
import org.drombler.media.management.config.model.json.MediaStorageConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
        MediaStorageType type = MediaStorageType.valueOf(configuration.getType());
        return new MediaStorage(configuration.getId(), configuration.getName(), Paths.get(configuration.getMediaRootDir()), type, configuration.getLegacyEventDirNames(), supportedMediaCategories);
    }

    private List<MediaCategory> getSupportedMediaCategories(MediaStorageConfiguration configuration) {
        return configuration.getSupportedMediaCategoryIds().stream()
                .filter(mediaCategoryId -> {
                    if (mediaCategoryManager.containsMediaCategory(mediaCategoryId)) {
                        return true;
                    } else {
                        LOGGER.error("Unknown mediaCategoryId: {}", mediaCategoryId);
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
        mediaStorageConfiguration.setSupportedMediaCategoryIds(getSupportedMediaCategoryIds(mediaStorage));
        // TODO: setPrivate
//        mediaStorageConfiguration.setPrivate();
        return mediaStorageConfiguration;
    }

    private static List<String> getSupportedMediaCategoryIds(MediaStorage mediaStorage) {
        return mediaStorage.getSupportedMediaCategories().stream()
                .map(MediaCategory::getId)
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
