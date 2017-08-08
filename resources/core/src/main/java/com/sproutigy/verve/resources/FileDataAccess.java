package com.sproutigy.verve.resources;

import com.sproutigy.verve.resources.io.LockType;
import com.sproutigy.verve.resources.io.LockableFileHandler;
import com.sproutigy.verve.resources.io.decorators.InputStreamDecorator;
import com.sproutigy.verve.resources.io.decorators.OutputStreamDecorator;
import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileDataAccess extends AbstractDataAccess {
    @Getter
    private File file;

    private LockableFileHandler fileHandler;

    public FileDataAccess(File file) {
        this.file = file;
        this.fileHandler = new LockableFileHandler(file);
    }

    @Override
    public long getPosition() throws IOException {
        return fileHandler.getChannel().position();
    }

    @Override
    public long getLength() throws IOException {
        return fileHandler.getChannel().size();
    }

    @Override
    public DataAccess setLength(long length) throws IOException {
        fileHandler.getChannelToWrite().truncate(length);
        return this;
    }

    @Override
    public boolean isSeekSupported() {
        return true;
    }

    @Override
    public DataAccess seek(long position) throws IOException {
        fileHandler.getChannel().position(position);
        return this;
    }

    @Override
    public DataAccess overwrite() throws IOException {
        fileHandler.overwrite();
        return this;
    }

    @Override
    public DataAccess append() throws IOException {
        fileHandler.append();;
        return this;
    }

    @Override
    public boolean isAtomicModeSupported() {
        return true;
    }

    @Override
    public boolean isAtomicMode() {
        return fileHandler.isAtomicMode();
    }

    @Override
    public void setAtomicMode(boolean atomicMode) {
        fileHandler.setAtomicMode(atomicMode);
    }

    @Override
    public boolean isLockSupported() {
        return true;
    }

    @Override
    public void lockShared() throws IOException {
        fileHandler.openReadable();
    }

    @Override
    public void lockExclusive() throws IOException {
        fileHandler.openWritable();
    }

    @Override
    public LockType getLockType() {
        return fileHandler.getLockType();
    }

    @Override
    public void unlock() throws IOException {
        fileHandler.close();
    }

    @Override
    public InputStream input() throws IOException {
        boolean managed;
        if (!fileHandler.isOpen()) {
            fileHandler.openReadable();
            managed = true;
        } else {
            managed = false;
        }

        return new InputStreamDecorator(fileHandler.inputStream()) {
            @Override
            public void close() throws IOException {
                super.close();
                if (managed) {
                    fileHandler.close();
                }
            }
        };
    }

    @Override
    public OutputStream output() throws IOException {
        boolean managed;
        if (!fileHandler.isOpen()) {
            managed = true;
        } else {
            managed = false;
        }

        return new OutputStreamDecorator(fileHandler.outputStream()) {
            @Override
            public void close() throws IOException {
                if (managed) {
                    fileHandler.commitAndClose();
                }
            }
        };
    }

    @Override
    public void close(boolean commit) throws Exception {
        if (commit) {
            fileHandler.commit();
        }
        fileHandler.close();
    }

    @Override
    public void close() throws Exception {
        fileHandler.close();
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            close(false);
        } catch (Throwable ignore) { }
    }
}
