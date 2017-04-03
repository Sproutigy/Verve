package com.sproutigy.verve.resources.fs;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.FileLock;

public class FileLockableInputStream extends FilterInputStream {

    private RandomAccessFile randomAccessFile;
    private FileLock fileLock;

    public FileLockableInputStream(File file) throws IOException {
        this(file, true);
    }

    public FileLockableInputStream(File file, boolean lock) throws IOException {
        super(null);

        if (lock) {
            randomAccessFile = new RandomAccessFile(file, "rw");
            fileLock = randomAccessFile.getChannel().lock(0, Long.MAX_VALUE, true);
            in = Channels.newInputStream(randomAccessFile.getChannel());
        }
        else {
            in = new FileInputStream(file);
        }
    }

    @Override
    public void close() throws IOException {
        if (fileLock != null) {
            fileLock.close();
            fileLock = null;
        }

        if (randomAccessFile != null) {
            randomAccessFile.getChannel().close();
            randomAccessFile.close();
            randomAccessFile = null;
        }
    }
}
