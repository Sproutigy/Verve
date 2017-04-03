package com.sproutigy.verve.resources.fs;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

public class ExtendedFileOutputStream extends OutputStream {

    private RandomAccessFile randomAccessFile;
    private FileLock fileLock;

    public ExtendedFileOutputStream(File file, boolean lock, boolean append, SyncMode syncMode) throws IOException {
        String mode;

        if (syncMode == SyncMode.SYNC_DATA) {
            mode = "rwd";
        }
        else if (syncMode == SyncMode.SYNC_DATA_AND_META) {
            mode = "rws";
        }
        else {
            mode = "rw";
        }

        randomAccessFile = new RandomAccessFile(file, mode);

        if (lock) {
            fileLock = randomAccessFile.getChannel().lock(0, Long.MAX_VALUE, true);
        }

        if (append) {
            randomAccessFile.seek((file.length())-1);
        }
    }

    public enum SyncMode {
        NO_SYNC, SYNC_DATA, SYNC_DATA_AND_META
    }

    @Override
    public void write(byte[] b) throws IOException {
        randomAccessFile.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        randomAccessFile.write(b, off, len);
    }

    @Override
    public void write(int b) throws IOException {
        randomAccessFile.write(b);
    }

    @Override
    public void flush() throws IOException {
        randomAccessFile.getFD().sync();
    }

    public RandomAccessFile getRandomAccessFile() {
        return randomAccessFile;
    }

    public FileChannel getFileChannel() {
        return randomAccessFile.getChannel();
    }

    @Override
    public void close() throws IOException {
        if (fileLock != null) {
            fileLock.close();
            fileLock = null;
        }

        if (randomAccessFile != null) {
            randomAccessFile.close();
            randomAccessFile = null;
        }
    }
}
