package com.sproutigy.verve.resources.props;

import com.sproutigy.commons.jsonright.jackson.JSON;
import com.sproutigy.verve.resources.io.LockingSupport;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

public interface JSONProps extends LockingSupport, AutoCloseable {

    <T> T get(String path, Class<? extends T> clazz) throws IOException;
    <T> T get(String path, T defaultValue) throws IOException;
    <T> T get(String path, Class<? extends T> clazz, T defaultValue) throws IOException;

    JSONProps set(Map<String, Object> mapPathValues) throws IOException;
    JSONProps set(String path, Object value) throws IOException;

    JSONProps remove(String path) throws IOException;
    JSONProps remove(Collection<String> paths) throws IOException;

    JSONProps clear() throws IOException;

    JSON fetch() throws IOException;
    JSONProps store(JSON props) throws IOException;

    boolean isEmpty() throws IOException;

}
