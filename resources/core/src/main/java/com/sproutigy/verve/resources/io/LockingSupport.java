package com.sproutigy.verve.resources.io;

import java.io.IOException;

public interface LockingSupport {
    boolean isLockSupported();
    void lockShared() throws IOException;
    void lockExclusive() throws IOException;
    LockType getLockType();
    void unlock() throws IOException;
}
