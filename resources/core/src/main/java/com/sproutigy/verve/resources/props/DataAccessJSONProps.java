package com.sproutigy.verve.resources.props;

import com.sproutigy.commons.binary.Binary;
import com.sproutigy.verve.resources.DataAccess;
import com.sproutigy.verve.resources.io.LockType;

import java.io.IOException;

public class DataAccessJSONProps extends AbstractJSONProps implements AutoCloseable {
    private DataAccess dataAccess;

    public DataAccessJSONProps(DataAccess dataAccess) {
        this.dataAccess = dataAccess;
        if (dataAccess.isAtomicModeSupported()) {
            dataAccess.setAtomicMode(false);
        }
        dataAccess.setSyncMode(DataAccess.SyncMode.SyncDataAndMeta);
    }

    public boolean isLockSupported() {
        return dataAccess.isLockSupported();
    }

    @Override
    public void lockShared() throws IOException {
        dataAccess.lockShared();
    }

    @Override
    public void lockExclusive() throws IOException {
        dataAccess.lockExclusive();
    }

    @Override
    public LockType getLockType() {
        return dataAccess.getLockType();
    }

    @Override
    public void unlock() throws IOException {
        dataAccess.unlock();
    }

    @Override
    protected synchronized byte[] loadRaw() throws IOException {
        return dataAccess.load().asByteArray(false);
    }

    @Override
    protected synchronized void saveRaw(byte[] data) throws IOException {
        dataAccess.save(Binary.from(data)).commit();
    }

    @Override
    public void close() throws Exception {
        if (dataAccess != null) {
            unlock();
            dataAccess.close();
            dataAccess = null;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            close();
        } catch (Exception ignore) { }
    }
}
