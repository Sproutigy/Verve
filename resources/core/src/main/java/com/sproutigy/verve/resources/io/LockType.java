package com.sproutigy.verve.resources.io;

public enum LockType {
    None,
    Shared,
    Exclusive;

    public boolean isSafelyReadable() {
        return this != None;
    }

    public boolean isSafelyWritable() {
        return this == Exclusive;
    }
}
