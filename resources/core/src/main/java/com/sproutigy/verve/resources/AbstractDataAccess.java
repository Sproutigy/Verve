package com.sproutigy.verve.resources;

import com.sproutigy.commons.binary.Binary;
import com.sproutigy.verve.resources.io.LockType;
import com.sproutigy.verve.resources.io.WriteMode;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class AbstractDataAccess implements DataAccess {

    private enum Seek {
        Here, Start, End
    }

    @Getter @Setter
    private SyncMode syncMode = SyncMode.SyncDataAndMeta;

    @Override
    public void setSyncMode(SyncMode syncMode) {
        this.syncMode = syncMode;
    }

    @Override
    public boolean isAtomicModeSupported() {
        return false;
    }

    @Override
    public boolean isAtomicMode() {
        return false;
    }

    @Override
    public void setAtomicMode(boolean atomicMode) {
    }

    @Override
    public boolean isLockSupported() {
        return false;
    }

    @Override
    public void lockShared() throws IOException {
    }

    @Override
    public void lockExclusive() throws IOException {
    }

    @Override
    public LockType getLockType() {
        return LockType.None;
    }

    @Override
    public void unlock() throws IOException {
    }

    @Override
    public synchronized Binary load() throws IOException {
        boolean ownsLock = false;
        if (isLockSupported() && (getLockType() == null || getLockType() == LockType.None)) {
            lockShared();
            ownsLock = true;
        }
        try {
            if (isSeekSupported()) {
                seekStart();
            }

            try(InputStream inputStream = input()) {
                return Binary.from(inputStream);
            }
        } finally {
            if (ownsLock) {
                unlock();
            }
        }
    }

    @Override
    public synchronized DataAccess save(Binary data) throws IOException {
        writeBinary(data, WriteMode.Overwrite);
        return this;
    }

    @Override
    public synchronized DataAccess append(Binary data) throws IOException {
        writeBinary(data, WriteMode.Append);
        return this;
    }

    @Override
    public void revert() throws IOException {
    }

    @Override
    public void commit() throws IOException {
    }

    @Override
    public DataAccess truncate() throws IOException {
        return setLength(0);
    }

    @Override
    public boolean isSeekSupported() {
        return false;
    }

    @Override
    public long getPosition() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getLength() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataAccess setLength(long length) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataAccess seek(long position) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataAccess seekStart() throws IOException {
        seek(0);
        return this;
    }

    @Override
    public DataAccess seekEnd() throws IOException {
        seek(getLength());
        return this;
    }

    @Override
    public DataAccess overwrite() throws IOException {
        setLength(0);
        seekStart();
        return this;
    }

    @Override
    public DataAccess append() throws IOException {
        seekEnd();
        return this;
    }

    private DataAccess writeBinary(Binary data, WriteMode writeMode) throws IOException {
        boolean ownsLock = false;
        if (isLockSupported()) {
            if (getLockType() == LockType.Shared) {
                throw new IllegalStateException("Shared lock already acquired");
            }
            if (getLockType() != LockType.Exclusive) {
                lockExclusive();
                ownsLock = true;
            }
        }
        if (writeMode == WriteMode.Overwrite) {
            overwrite();
        } else if (writeMode == WriteMode.Append) {
            append();
        }

        try {
            try (OutputStream outputStream = output()) {
                data.toStream(outputStream);
            }

            if (getPosition() < getLength()) {
                setLength(getPosition());
            }

            return this;

        } finally {
            if (ownsLock) {
                unlock();
            }
        }
    }

    @Override
    public void close(boolean commit) throws Exception {
        if (commit) {
            commit();
        }
        close();
    }

    @Override
    public void close() throws Exception {
        close(false);
    }

}
