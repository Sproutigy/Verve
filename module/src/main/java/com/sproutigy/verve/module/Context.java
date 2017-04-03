package com.sproutigy.verve.module;

import java.util.Collection;

public interface Context {
    Context getParent();

    void add(String type, Object instance);
    <T> void add(Class<T> clazz, T instance);

    void set(String type, Object instance);
    <T> void set(Class<T> clazz, T instance);

    void remove(String type);
    <T> void remove(Class<T> clazz);

    void remove(String type, Object instance);
    <T> void remove(Class<T> clazz, Object instance);

    <T> T get(Class<T> clazz);
    <T> T get(String type);

    <T> Collection<T> getAll(Class<T> clazz);
    <T> Collection<T> getAll(String type);
}
