package com.sproutigy.verve.resources;

import com.sproutigy.verve.resources.fs.ExtendedFileOutputStream;
import com.sproutigy.verve.resources.fs.FSUtils;
import com.sproutigy.verve.resources.fs.FileLockableInputStream;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class FileResource extends AbstractResource {
    protected File file;

    public FileResource(File file) {
        this.file = file;
    }

    public FileResource(String localPath) {
        this(new File(localPath));
    }

    public FileResource(Path path) {
        this(path.toFile());
    }

    @Override
    public String getName() {
        return file.getName();
    }

    @Override
    public String getDescriptor() {
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            return file.getAbsolutePath();
        }
    }

    @Override
    public boolean exists() throws IOException {
        return file.exists();
    }

    @Override
    public InputStream getInputStream(ReadOption... options) throws IOException {
        boolean lock = hasOption(options, ReadOption.LOCK);
        return new FileLockableInputStream(file, lock);
    }

    @Override
    public Optional<Resource> getParent() {
        File parentFile = file.getParentFile();
        if (parentFile == null) {
            return Optional.empty();
        }
        return Optional.of(new FileResource(parentFile));
    }

    @Override
    public boolean create() throws IOException {
        return file.createNewFile();
    }

    @Override
    public boolean isContainer() {
        return file.isDirectory();
    }

    @Override
    public boolean isFile() {
        return file.isFile();
    }

    @Override
    protected Resource resolveChild(String name) {
        return resolveChild(new File(file, name));
    }

    protected Resource resolveChild(File childFile) {
        return new FileResource(childFile);
    }

    @Override
    public OutputStream getOutputStream(WriteOption... options) throws IOException {
        boolean lock = hasOption(options, WriteOption.LOCK);
        boolean append = hasOption(options, WriteOption.APPEND);

        if (hasOption(options, WriteOption.ATOMIC)) {
            File tempFile;
            do {
                tempFile = new File(file.getParentFile(), "~"+file.getName()+"~"+System.currentTimeMillis()+".tmp");
            } while(!tempFile.createNewFile());

            final File target = tempFile;

            try (InputStream inputStream = getInputStream(ReadOption.LOCK)) {
                if (append) {
                    Files.copy(inputStream, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }

                return new FileOutputStream(target, append) {
                    @Override
                    public void close() throws IOException {
                        super.close();
                        executor.execute(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Thread.sleep(0);
                                } catch (InterruptedException e) { }
                                try {
                                    inputStream.close();
                                } catch (IOException e) { }
                            }
                        });
                        FSUtils.movePreferAtomic(target.toPath(), file.toPath());
                    }
                };
            }
        }
        else {
            ExtendedFileOutputStream.SyncMode syncMode;

            if (hasOption(options, WriteOption.SYNC_DATA_AND_META)) {
                syncMode = ExtendedFileOutputStream.SyncMode.SYNC_DATA_AND_META;
            } else if (hasOption(options, WriteOption.SYNC_DATA)) {
                syncMode = ExtendedFileOutputStream.SyncMode.SYNC_DATA;
            } else {
                syncMode = ExtendedFileOutputStream.SyncMode.NO_SYNC;
            }

            return new ExtendedFileOutputStream(file, lock, append, syncMode);
        }
    }

    @Override
    public boolean delete(DeleteOption... options) throws IOException {
        boolean ignoreExceptions = hasOption(options, DeleteOption.IGNORE_EXCEPTIONS);
        try {
            if (!hasOption(options, DeleteOption.INCLUDE_CHILDREN)) {
                return Files.deleteIfExists(file.toPath());
            } else {
                if (ignoreExceptions) {
                    return FSUtils.deleteWithChildrenIgnoreExceptions(file);
                } else {
                    return FSUtils.deleteWithChildren(file);
                }
            }
        } catch(IOException e) {
            if (ignoreExceptions) {
                return false;
            }
            throw e;
        }
    }

    @Override
    public Optional<File> toLocalFile() throws IOException {
        return Optional.of(file);
    }

    @Override
    public Optional<Path> toFilePath() {
        return Optional.of(file.toPath());
    }

    @Override
    public boolean isModifiable() {
        return Files.isWritable(file.toPath());
    }

    @Override
    public Iterable<Resource> getChildren() throws IOException {
        File[] childs = file.listFiles();
        if (childs == null) {
            return Collections.emptySet();
        }
        List<Resource> resources = new LinkedList<>();
        for (File child : childs) {
            if (!(child.getName().startsWith("~") && child.getName().endsWith(".tmp"))) {
                resources.add(resolveChild(child));
            }
        }
        return Collections.unmodifiableList(resources);
    }

    private boolean hasOption(Object[] options, Object option) {
        for(Object opt : options) {
            if (opt == option) {
                return true;
            }
        }
        return false;
    }
}
