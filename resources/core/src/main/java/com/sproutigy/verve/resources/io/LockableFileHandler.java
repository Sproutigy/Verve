package com.sproutigy.verve.resources.io;

import com.sproutigy.verve.resources.io.decorators.InputStreamDecorator;
import com.sproutigy.verve.resources.io.decorators.OutputStreamDecorator;
import com.sproutigy.verve.resources.io.decorators.ReaderDecorator;
import com.sproutigy.verve.resources.io.decorators.WriterDecorator;
import lombok.SneakyThrows;

import java.io.*;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * LockableFileHandler maintains locks on file, optionally support atomical changes
 *
 * Not thread safe!
 */
public class LockableFileHandler implements AtomicModeSupport, AutoCloseable {
    private Path filePath;
    private Path editFilePath;
    private Path lockFilePath;
    private FileChannel fileChannel;
    private FileChannel lockFileChannel;
    private FileChannel editFileChannel;
    private FileLock lock;
    private FileLock lockFileLock;
    private FileLock editFileLock;
    private boolean atomicMode = false;
    private boolean exclusive = false;
    private WriteMode writeMode;
    private SyncMode syncMode;

    private static final String DOS_HIDDEN_ATTRIBUTE = "dos:hidden";

    private static final AutoRetrier AUTO_RETRIER = new AutoRetrier(30, TimeUnit.SECONDS) {
        @Override
        public boolean shouldRetry(Throwable e) {
            if (e instanceof FileSystemException && e.getMessage().contains("used by another process")) {
                return true;
            }
            if (e instanceof OverlappingFileLockException) {
                return true;
            }
            if (e instanceof AccessDeniedException) {
                return true;
            }
            return false;
        }
    };

    public LockableFileHandler(File file) {
        this(file.toPath());
    }

    public LockableFileHandler(Path filePath) {
        this.filePath = filePath;
        lockFilePath = Paths.get(this.filePath.getParent().toString(), ".~" + this.filePath.getFileName() + ".lock");
        editFilePath = Paths.get(this.filePath.getParent().toString(), ".~" + this.filePath.getFileName() + ".edit");
    }

    @Override
    public boolean isAtomicModeSupported() {
        return true;
    }

    @Override
    public boolean isAtomicMode() {
        return atomicMode;
    }

    @Override
    public void setAtomicMode(boolean atomicMode) {
        if (isOpen()) {
            throw new IllegalStateException("Cannot change atomic mode while open");
        }
        this.atomicMode = atomicMode;
    }

    public SyncMode getSyncMode() {
        return syncMode;
    }

    public void setSyncMode(SyncMode syncMode) {
        this.syncMode = syncMode;
    }

    public File getFile() {
        return getFilePath().toFile();
    }

    public Path getFilePath() {
        return filePath;
    }

    protected Path getLockFilePath() {
        return lockFilePath;
    }

    protected Path getEditFilePath() {
        return editFilePath;
    }

    public boolean isOpen() {
        return fileChannel != null || lockFileLock != null;
    }

    public LockType getLockType() {
        if (lockFileChannel != null) {
            if (lockFileLock != null && lockFileLock.isValid()) {
                if (lockFileLock.isShared()) return LockType.Shared;
                if (!lockFileLock.isShared()) return LockType.Exclusive;
            }
        } else {
            if (lock != null && lock.isValid()) {
                if (lock.isShared()) return LockType.Shared;
                if (!lock.isShared()) return LockType.Exclusive;
            }
        }
        return LockType.None;
    }

    public LockableFileHandler openReadable() throws IOException {
        open(false);
        return this;
    }


    public LockableFileHandler openWritable() throws IOException {
        open(true);
        return this;
    }

    public void overwrite() throws IOException {
        validateOpenToWrite();
        writeMode = WriteMode.Overwrite;
        getChannelToWrite().truncate(0);
    }

    public void append() throws IOException {
        validateOpenToWrite();
        writeMode = WriteMode.Append;
        getChannelToWrite().position(getChannel().size());
    }

    public boolean exists() {
        return Files.exists(getFilePath());
    }

    private void open(boolean exclusive) throws IOException {
        if (isOpen()) {
            if (this.exclusive == exclusive) {
                return;
            }
        }

        this.exclusive = exclusive;

        if (isAtomicMode()) {
            openLockFile(exclusive);
        }
        openFile(exclusive);
    }

    @SneakyThrows
    private FileChannel openChannel(Path path, boolean createIfNotExists, Collection<OpenOption> options) throws IOException {
        FileChannel channel = AUTO_RETRIER.call(() -> {
            try {
                return FileChannel.open(path, options.toArray(new OpenOption[options.size()]));
            } catch (NoSuchFileException noSuchFile) {
                if (createIfNotExists) {
                    Files.createFile(getFilePath());
                    return openChannel(path, true, options);
                } else {
                    throw noSuchFile;
                }
            }
        });

        return channel;
    }

    private void openFile(boolean exclusive) throws IOException {
        List<OpenOption> openOptions = new LinkedList<>();
        openOptions.add(StandardOpenOption.READ);
        openOptions.add(StandardOpenOption.CREATE);
        //openOptions.add(ExtendedOpenOption.NOSHARE_DELETE);

        if (exclusive) {
            openOptions.add(StandardOpenOption.WRITE);
        }

        fileChannel = openChannel(getFilePath(), exclusive, openOptions);
        lock = provideLock(fileChannel, exclusive);
    }

    private void closeFile() throws IOException {
        if (lock != null) {
            closeSilently(lock);
            lock = null;
        }
        if (fileChannel != null) {
            closeSilently(fileChannel);
            fileChannel = null;
        }
    }

    private void openLockFile(boolean exclusive) throws IOException {
        if (lockFileChannel != null) {
            closeLockFile();
        }

        List<OpenOption> openOptions = new LinkedList<>();
        openOptions.add(StandardOpenOption.READ);
        openOptions.add(StandardOpenOption.WRITE);
        openOptions.add(StandardOpenOption.CREATE);
        //openOptions.add(ExtendedOpenOption.NOSHARE_DELETE);
        openOptions.add(StandardOpenOption.DELETE_ON_CLOSE);

        lockFileChannel = openChannel(getLockFilePath(), true, openOptions);
        hide(lockFilePath);
        lockFileLock = provideLock(lockFileChannel, exclusive);
    }

    private void closeLockFile() throws IOException {
        if (lockFileLock != null) {
            closeSilently(lockFileLock);
            lockFileLock = null;
        }
        if (lockFileChannel != null) {
            closeSilently(lockFileChannel);
            lockFileChannel = null;
        }

        tryDeleteFile(lockFilePath);
    }

    private void openEditFile() throws IOException {
        boolean prepared = true;

        if (editFileChannel == null) {
            if (writeMode != WriteMode.Overwrite) {
                try {
                    Files.copy(getFilePath(), getEditFilePath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                } catch (IOException ignore) {
                    prepared = false;
                }
            }
        } else {
            closeSilently(editFileLock);
            closeSilently(editFileChannel);
        }

        List<OpenOption> openOptions = new LinkedList<>();
        openOptions.add(StandardOpenOption.READ);
        openOptions.add(StandardOpenOption.WRITE);
        //openOptions.add(ExtendedOpenOption.NOSHARE_DELETE);
        if (writeMode == WriteMode.Overwrite) {
            openOptions.add(StandardOpenOption.CREATE_NEW);
        }

        editFileChannel = openChannel(getEditFilePath(), true, openOptions);
        hide(editFilePath);
        editFileLock = provideLock(editFileChannel, true);

        if (!prepared) {
            long filePos = fileChannel.position();
            fileChannel.position(0);
            copyFileChannels(fileChannel, editFileChannel);
            fileChannel.position(filePos);
            editFileChannel.position(0);
        }

        if (writeMode == WriteMode.Append) {
            editFileChannel.position(fileChannel.size());
        }
    }

    private void closeEditFile() throws IOException {
        if (editFileLock != null) {
            closeSilently(editFileLock);
            editFileLock = null;
        }
        if (editFileChannel != null) {
            closeSilently(editFileChannel);
            editFileChannel = null;
        }
    }

    private boolean tryDeleteFile(Path path) {
        try {
            if (Files.exists(path)) {
                if (!Files.deleteIfExists(path)) {
                    path.toFile().deleteOnExit();
                    return false;
                }
            }

            return true;
        } catch (Exception ignore) { return false; }
    }

    @SneakyThrows
    public void replaceByMove(Path source, boolean copyAttributes) throws IOException {
        validateOpenToWrite();

        closeFile();

        AUTO_RETRIER.call(() -> {
            try {
                if (copyAttributes) {
                    Files.move(source, getFilePath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.COPY_ATTRIBUTES);
                } else {
                    Files.move(source, getFilePath(), StandardCopyOption.ATOMIC_MOVE);
                }
            } catch (AtomicMoveNotSupportedException e) {
                if (copyAttributes) {
                    Files.move(source, getFilePath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                } else {
                    Files.move(source, getFilePath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
            return null;
        });

        revert();
    }

    public void replaceByCopy(Path source, boolean copyAttributes) throws IOException {
        validateOpenToWrite();

        revert();
        closeFile();

        if (copyAttributes) {
            Files.copy(source, getEditFilePath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
        } else {
            Files.copy(source, getEditFilePath(), StandardCopyOption.REPLACE_EXISTING);
        }

        commit(copyAttributes);

        if (!isOpen()) {
            openWritable();
        }
    }

    protected static void copyFileChannels(FileChannel source, FileChannel target) throws IOException {
        final long size = source.size();
        long pos = 0;
        while (pos < size) {
            pos += target.transferFrom(source, pos, Long.MAX_VALUE);
        }
    }

    @SneakyThrows
    private FileLock provideLock(FileChannel fileChannel, boolean exclusive) throws IOException {
        return AUTO_RETRIER.call(() -> fileChannel.lock(0, Long.SIZE, !exclusive));
    }

    public FileChannel getChannel() {
        if (editFileChannel != null) {
            return editFileChannel;
        }
        if (fileChannel != null) {
            return fileChannel;
        }
        throw new IllegalStateException("File is not open");
    }

    public FileChannel getChannelToRead() throws IOException {
        FileChannel channel = getChannel();
        if (channel == null) {
            openFile(false);
            channel = fileChannel;
        }
        return channel;
    }

    public FileChannel getChannelToWrite() throws IOException {
        FileChannel result;
        if (!isAtomicMode()) {
            result = fileChannel;
        } else {
            result = editFileChannel;
        }

        if (result == null || !result.isOpen()) {
            if (isAtomicMode()) {
                openEditFile();
                result = editFileChannel;
            } else {
                openFile(true);
                result = fileChannel;
            }
        }
        return result;
    }

    public void flush() throws IOException {
        if (writeMode != null) {
            if (editFileChannel != null) {
                editFileChannel.force(false);
            }
            else if (fileChannel != null) {
                fileChannel.force(true);
            }
        }
    }

    public InputStream inputStream() throws IOException {
        return new InputStreamDecorator(Channels.newInputStream(getChannelToRead())) {
            @Override
            public void close() throws IOException {
            }
        };
    }

    public OutputStream outputStream() throws IOException {
        return new OutputStreamDecorator(Channels.newOutputStream(getChannelToWrite())) {
            @Override
            public void close() throws IOException {
                try {
                    flush();
                } catch (NonWritableChannelException e) {
                    LockableFileHandler.this.flush();
                }
            }
        };
    }

    public Reader reader(Charset charset) throws IOException {
        return new ReaderDecorator(Channels.newReader(getChannelToRead(), charset.name())) {
            @Override
            public void close() throws IOException {
            }
        };
    }

    public Writer writer(Charset charset) throws IOException {
        return new WriterDecorator(Channels.newWriter(getChannelToWrite(), charset.name())) {
            @Override
            public void close() throws IOException {
                try {
                    flush();
                } catch (NonWritableChannelException e) {
                    LockableFileHandler.this.flush();
                }
            }
        };
    }

    public void commit() throws IOException {
        commit(false);
    }

    public void commitAndClose() throws IOException {
        commit();
        close();
    }

    public void revert() throws IOException {
        if (isAtomicMode()) {
            closeEditFile();
            tryDeleteFile(getEditFilePath());
        }
    }

    private void commit(boolean copyAttributes) throws IOException {
        if (isAtomicMode()) {
            closeEditFile();
            if (Files.exists(getEditFilePath())) {
                unhide(getEditFilePath());
                replaceByMove(getEditFilePath(), copyAttributes);
            }
        }

        sync();
    }

    public void sync() throws IOException {
        if (getSyncMode() != null && getSyncMode() != SyncMode.Default) {
            boolean syncMetaData = getSyncMode() == SyncMode.SyncDataAndMeta;
            if (fileChannel != null && fileChannel.isOpen()) {
                fileChannel.force(syncMetaData);
            }
        }
    }

    public boolean delete() throws IOException {
        revert();
        closeFile();
        return Files.deleteIfExists(getFilePath());
    }

    @Override
    public void close() throws IOException {
        if (writeMode != null) {
            closeEditFile();
            tryDeleteFile(getEditFilePath());
        }
        sync();
        closeFile();
        closeLockFile();
        this.writeMode = null;
        this.exclusive = false;
    }

    private void validateOpen() {
        if (!isOpen()) {
            throw new IllegalStateException("Not open");
        }
    }

    private void validateOpenToWrite() {
        validateOpen();
        if (!exclusive) {
            throw new IllegalStateException("Not exclusively open");
        }
    }

    private static void closeSilently(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception ignore) {
            }
        }
    }

    private static void hide(Path path) {
        try {
            Files.setAttribute(path, DOS_HIDDEN_ATTRIBUTE, true);
        } catch (Exception ignore) { }
    }

    private static void unhide(Path path) {
        try {
            Files.setAttribute(path, DOS_HIDDEN_ATTRIBUTE, false);
        } catch (Exception ignore) { }
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            close();
        } catch (Exception ignore) { }
    }
}
