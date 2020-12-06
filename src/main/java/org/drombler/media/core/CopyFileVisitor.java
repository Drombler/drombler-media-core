/*
 *         COMMON DEVELOPMENT AND DISTRIBUTION LICENSE (CDDL) Notice
 *
 * The contents of this file are subject to the COMMON DEVELOPMENT AND DISTRIBUTION LICENSE (CDDL)
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.opensource.org/licenses/cddl1.txt
 *
 * The Original Code is SoftSmithy Utility Library. The Initial Developer of the
 * Original Code is Florian Brunner (Sourceforge.net user: puce). All Rights Reserved.
 *
 * Contributor(s): .
 */
package org.drombler.media.core;

import org.softsmithy.lib.nio.file.PathUtils;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * A {@link FileVisitor} which copies the source {@link Path} to the target {@link Path}. <br>
 * <br>
 * If the source is a directory, it content gets copied recursively. <br>
 * <br>
 * The source and the target don't have to be on the same file system and thus this class can be used to e.g. extract a
 * directory from a jar/ zip file.
 *
 * @author puce
 * @see Files#walkFileTree(Path, FileVisitor)
 * @see #copy(Path, Path, CopyOption...)
 */
public class CopyFileVisitor extends SimpleFileVisitor<Path> {

    private final Path source;
    private final Path target;
    private final FileMigrationOperation fileMigrationOperation;
    private final CopyOption[] options;
    private final Set<CopyOption> optionsSet;

    /**
     * Creates a new instance of this class.
     *
     * @param source  the source
     * @param target  the target
     * @param options copy options
     * @see #copy(Path, Path, CopyOption...)
     */
    public CopyFileVisitor(Path source, Path target, CopyOption... options) {
        this(source, target, FileMigrationOperation.COPY, options);
    }

    /**
     * Creates a new instance of this class.
     *
     * @param source  the source
     * @param target  the target
     * @param options copy options
     * @see #copy(Path, Path, CopyOption...)
     */
    public CopyFileVisitor(Path source, Path target, FileMigrationOperation fileMigrationOperation, CopyOption... options) {
        this.source = source;
        this.target = target;
        this.fileMigrationOperation = fileMigrationOperation;
        this.options = options;
        this.optionsSet = new HashSet<>(Arrays.asList(options));
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        Path targetDir = PathUtils.resolve(target, source.relativize(dir));
        try {
            if (!Files.exists(targetDir)) {
                Files.copy(dir, targetDir, options); // also for move we have to use copy to copy the dir name
            }
        } catch (FileAlreadyExistsException e) {
            if (!Files.isDirectory(targetDir)) {
                throw e;
            }
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        super.postVisitDirectory(dir, exc);
        if (fileMigrationOperation.isDeletingSource()) {
            Files.delete(dir);
        }
        return FileVisitResult.CONTINUE;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Path targetFile = PathUtils.resolve(target, source.relativize(file));
        if (!Files.exists(targetFile) || optionsSet.contains(StandardCopyOption.REPLACE_EXISTING)) {
            fileMigrationOperation.migrate(file, targetFile, options);
        }
        return FileVisitResult.CONTINUE;
    }

    /**
     * Copies the source to the target. <br>
     * <br>
     * If the source is a directory, its content gets copied recursively. <br>
     * <br>
     * The source and the target don't have to be on the same file system and thus this method can be used to e.g.
     * extract a directory from a jar/ zip file.
     *
     * @param source  the source
     * @param target  the target
     * @param options the copy options
     * @return the source
     * @throws IOException if an I/O error is thrown
     */
    public static Path copy(Path source, Path target, CopyOption... options) throws IOException {
        return Files.walkFileTree(source, new CopyFileVisitor(source, target, FileMigrationOperation.COPY, options));
    }

    /**
     * Moves the source to the target. <br>
     * <br>
     * If the source is a directory, its content gets copied recursively. <br>
     * <br>
     * The source and the target don't have to be on the same file system and thus this method can be used to e.g.
     * extract a directory from a jar/ zip file.
     *
     * @param source  the source
     * @param target  the target
     * @param options the copy options
     * @return the source
     * @throws IOException if an I/O error is thrown
     */
    public static Path move(Path source, Path target, CopyOption... options) throws IOException {
        return Files.walkFileTree(source, new CopyFileVisitor(source, target, FileMigrationOperation.MOVE, options));
    }
}
