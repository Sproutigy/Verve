package com.sproutigy.verve.resources.fs;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicReference;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.TERMINATE;

public class FSUtils {

    private FSUtils() { }

    public static boolean deleteWithChildren(File fileOrDirectory) throws IOException {
        return deleteWithChildren(fileOrDirectory, false);
    }

    public static boolean deleteWithChildrenIgnoreExceptions(File fileOrDirectory) {
        try {
            return deleteWithChildren(fileOrDirectory, true);
        } catch(IOException ignore) {
            return false;
        }
    }

    private static boolean deleteWithChildren(File fileOrDirectory, boolean ignoreExceptions) throws IOException {
        if (!fileOrDirectory.exists()) {
            return false;
        }

        File[] children = fileOrDirectory.listFiles();
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory()) {
                    deleteWithChildren(child, ignoreExceptions);
                } else {
                    Files.deleteIfExists(child.toPath());
                }
            }
        }

        try {
            return Files.deleteIfExists(fileOrDirectory.toPath());
        } catch(IOException e) {
            if (!ignoreExceptions) {
                throw e;
            }
            return false;
        }
    }

    public static boolean deleteWithChildren(Path path) throws IOException {
        return deleteWithChildren(path, false);
    }

    public static boolean deleteWithChildrenIgnoreExceptions(Path path) {
        try {
            return deleteWithChildren(path, true);
        } catch (IOException ignore) {
            return false;
        }
    }

    private static boolean deleteWithChildren(Path path, boolean ignoreExceptions) throws IOException {
        AtomicReference<IOException> exception = new AtomicReference<>();

        if (!Files.exists(path)) {
            return false;
        }

        Files.walkFileTree(path, new SimpleFileVisitor<Path>(){
            @Override public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file);
                return CONTINUE;
            }

            @Override public FileVisitResult visitFileFailed(final Path file, final IOException e) {
                return handleException(e);
            }


            @Override public FileVisitResult postVisitDirectory(final Path dir, final IOException e)
                    throws IOException {
                if (e != null) {
                    return handleException(e);
                }
                Files.deleteIfExists(dir);
                return CONTINUE;
            }

            private FileVisitResult handleException(final IOException e) {
                exception.set(e);
                if (ignoreExceptions) {
                    return CONTINUE;
                } else {
                    return TERMINATE;
                }
            }
        });

        if (exception.get() != null) {
            if (!ignoreExceptions) {
                throw exception.get();
            }

            return false;
        }

        return true;
    }

    public static void movePreferAtomic(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch(AtomicMoveNotSupportedException e) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
