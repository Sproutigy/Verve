package com.sproutigy.verve.resources.props;

import com.sproutigy.commons.jsonright.jackson.JSON;
import com.sproutigy.verve.resources.io.LockType;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public abstract class AbstractJSONProps implements JSONProps, AutoCloseable {

    public <T> T get(String path, Class<? extends T> clazz) throws IOException {
        return get(path, clazz, null);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String path, T defaultValue) throws IOException {
        return get(path, (Class<? extends T>)defaultValue.getClass(), defaultValue);
    }

    public <T> T get(String path, Class<? extends T> clazz, T defaultValue) throws IOException {
        JSON data = fetch();
        return data.get(path, clazz, defaultValue);
    }

    public JSONProps set(Map<String, Object> mapPathValues) throws IOException {
        boolean ownLock = false;
        if (isLockSupported() && getLockType() != LockType.Exclusive) {
            lockExclusive();
            ownLock = true;
        }
        try {
            JSON data = fetch();
            for (Map.Entry<String, Object> entry : mapPathValues.entrySet()) {
                data.set(entry.getKey(), entry.getValue());
            }
            store(data);
        } finally {
            if (ownLock) {
                unlock();
            }
        }
        return this;
    }

    public JSONProps set(String path, Object value) throws IOException {
        return set(Collections.singletonMap(path, value));
    }

    public JSONProps remove(String path) throws IOException {
        return remove(Collections.singleton(path));
    }

    @Override
    public JSONProps remove(Collection<String> paths) throws IOException {
        boolean ownLock = false;
        if (isLockSupported() && getLockType() != LockType.Exclusive) {
            lockExclusive();
            ownLock = true;
        }

        try {
            JSON data = fetch();
            for (String path : paths) {
                data.remove(path);
            }
            store(data);
        } finally {
            if (ownLock) {
                unlock();
            }
        }
        return this;
    }

    @Override
    public JSONProps clear() throws IOException {
        boolean ownLock = false;
        if (isLockSupported() && getLockType() != LockType.Exclusive) {
            lockExclusive();
            ownLock = true;
        }

        try {
            store(JSON.newNull());
        } finally {
            if (ownLock) {
                unlock();
            }
        }
        return this;
    }

    @Override
    public JSON fetch() throws IOException {
        byte[] data = loadRaw();
        if (isEmpty(data)) {
            return JSON.newObject();
        }
        return JSON.fromBytes(data);
    }

    @Override
    public JSONProps store(JSON props) throws IOException {
        if (props == null) {
            saveRaw(null);
        } else {
            byte[] data = props.toStringPretty().getBytes(Charset.forName("UTF-8"));
            saveRaw(data);
        }
        return this;
    }

    @Override
    public boolean isEmpty() throws IOException {
        return isEmpty(loadRaw());
    }

    private boolean isEmpty(byte[] data) {
        return data == null || data.length == 0 || (data.length == 4 && data[0] == 'n' && data[1] == 'u' && data[2] == 'l' && data[3] == 'l');
    }

    @Override
    public boolean isLockSupported() {
        return false;
    }

    @Override
    public void lockShared() throws IOException {
        throw new UnsupportedOperationException("Locking not supported");
    }

    @Override
    public void lockExclusive() throws IOException {
        throw new UnsupportedOperationException("Locking not supported");
    }

    @Override
    public LockType getLockType() {
        return LockType.None;
    }

    @Override
    public void unlock() throws IOException {
        throw new UnsupportedOperationException("Unlocking not supported");
    }

    protected abstract byte[] loadRaw() throws IOException;
    protected abstract void saveRaw(byte[] data) throws IOException;

    @Override
    public void close() throws Exception {
        if (isLockSupported() && getLockType() != LockType.None) {
            unlock();
        }
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            close();
        } catch (Throwable ignore) { }
    }
}
