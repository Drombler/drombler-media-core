package org.drombler.media.core;

import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;

public enum FileMigrationOperation {
    COPY(false) {
        @Override
        public void migrate(Path sourceFilePath, Path targetFilePath, CopyOption... copyOptions) throws IOException {
            Files.copy(sourceFilePath, targetFilePath, copyOptions);
        }
    },
    MOVE(true) {
        @Override
        public void migrate(Path sourceFilePath, Path targetFilePath, CopyOption... copyOptions) throws IOException {
            Files.move(sourceFilePath, targetFilePath, copyOptions);
        }
    };

    private boolean deletingSource;

    FileMigrationOperation(boolean deletingSource) {
        this.deletingSource = deletingSource;
    }

    public abstract void migrate(Path sourceFilePath, Path targetFilePath, CopyOption... copyOptions) throws IOException;

    public boolean isDeletingSource() {
        return deletingSource;
    }
}
