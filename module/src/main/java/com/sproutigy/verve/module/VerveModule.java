package com.sproutigy.verve.module;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;

public interface VerveModule extends Module, AutoCloseable {
    String getName();
    void setContext(Context context);
    Context getContext();

    <T> T getInstance(Class<T> clazz);
    <T> T getInstance(Key<T> key);

    void postInject(Context context, Injector injector) throws Exception;
}
