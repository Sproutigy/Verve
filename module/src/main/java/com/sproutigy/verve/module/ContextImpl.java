package com.sproutigy.verve.module;

import lombok.Getter;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ContextImpl implements Context {
    @Getter
    private Context parent;

    private Map<String, Collection<Object>> data = new ConcurrentHashMap<>();

    public ContextImpl() { }

    public ContextImpl(Context parent) {
        this.parent = parent;
    }

    @Override
    public void add(String type, Object instance) {
        Collection<Object> col = data.computeIfAbsent(type, x -> new CopyOnWriteArrayList<>());
        col.add(instance);
    }

    @Override
    public <T> void add(Class<T> clazz, T instance) {
        add(classToType(clazz), instance);
    }

    @Override
    public synchronized void set(String type, Object instance) {
        data.put(type, Collections.singleton(instance));
    }

    @Override
    public <T> void set(Class<T> clazz, T instance) {
        set(classToType(clazz), instance);
    }

    @Override
    public void remove(String type) {
        data.remove(type);
    }

    @Override
    public <T> void remove(Class<T> clazz) {
        remove(classToType(clazz));
    }

    @Override
    public void remove(String type, Object instance) {
        data.computeIfPresent(type, (t, objects) -> {
            objects.remove(instance);
            return objects;
        });
    }

    @Override
    public <T> void remove(Class<T> clazz, Object instance) {
        remove(classToType(clazz), instance);
    }

    @Override
    public <T> T get(Class<T> clazz) {
        return get(classToType(clazz));
    }

    @Override
    public <T> T get(String type) {
        Collection<T> col = getAll(type);
        if (col.isEmpty()) {
            if (parent != null) {
                return (T)parent.get(type);
            }
            return null;
        }
        return col.iterator().next();
    }

    @Override
    public <T> Collection<T> getAll(Class<T> clazz) {
        return getAll(classToType(clazz));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Collection<T> getAll(String type) {
        Collection result;
        Collection thisCol = data.get(type);
        Collection parentCol = null;
        if (parent != null) {
            parentCol = parent.get(type);
            if (parentCol != null && parentCol.isEmpty()) {
                parentCol = null;
            }
        }
        if (thisCol == null) {
            if (parentCol == null) {
                return Collections.emptyList();
            }
            result = parentCol;
        } else {
            if (parentCol == null) {
                result = thisCol;
            } else {
                result = new LinkedList();
                result.addAll(parentCol);
                result.addAll(thisCol);
            }
        }
        return (Collection<T>)Collections.unmodifiableCollection(result);

    }

    private String classToType(Class clazz) {
        return clazz.getSimpleName();
    }
}
