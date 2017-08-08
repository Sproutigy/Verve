package com.sproutigy.verve.resources;

import com.sproutigy.commons.binary.Binary;
import com.sproutigy.verve.resources.io.AtomicModeSupport;
import com.sproutigy.verve.resources.io.LockingSupport;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface DataAccess extends AtomicModeSupport, LockingSupport, AutoCloseable {
    enum SyncMode {
        NoSync, SyncData, SyncDataAndMeta
    }

    void setSyncMode(SyncMode syncMode);
    SyncMode getSyncMode();

    long getPosition() throws IOException;
    long getLength() throws IOException;
    DataAccess setLength(long length) throws IOException;
    DataAccess truncate() throws IOException;

    boolean isSeekSupported();
    DataAccess seek(long position) throws IOException;
    DataAccess seekStart() throws IOException;
    DataAccess seekEnd() throws IOException;

    InputStream input() throws IOException;
    OutputStream output() throws IOException;

    DataAccess overwrite() throws IOException;
    DataAccess append() throws IOException;

    Binary load() throws IOException;
    DataAccess save(Binary data) throws IOException;
    DataAccess append(Binary data) throws IOException;

    void revert() throws IOException;
    void commit() throws IOException;

    void close(boolean commit) throws Exception;
}
