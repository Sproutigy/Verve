package com.sproutigy.verve.resources.io;

public interface AtomicModeSupport {
    boolean isAtomicModeSupported();
    boolean isAtomicMode();
    void setAtomicMode(boolean atomicMode);
}
