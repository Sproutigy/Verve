package com.sproutigy.verve.module;

public interface VerveRootListener {
    enum EventType {
        Initializing,
        Injected,
        Initialized,
        Destroying,
        Destroyed
    }

    void onModuleEvent(EventType eventType, VerveModule module);
}
