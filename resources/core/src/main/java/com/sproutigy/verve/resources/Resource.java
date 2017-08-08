package com.sproutigy.verve.resources;

import com.sproutigy.verve.resources.props.JSONProps;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.Optional;

public interface Resource extends Iterable<Resource>, AutoCloseable {
    char PATH_SEPARATOR = '/';

    String getName();
    String getDescriptor();

    boolean hasParent() throws IOException;
    Optional<? extends Resource> getParent() throws IOException;

    Resource getRoot() throws IOException;

    boolean create() throws IOException;
    boolean createContainer() throws IOException;

    boolean exists() throws IOException;

    boolean isContainer();
    boolean isFile();

    boolean hasChild(String name) throws IOException;
    Resource child(String name) throws IOException;

    boolean hasChildren();
    Iterable<? extends Resource> getChildren() throws IOException;
    Iterable<? extends Resource> getChildren(boolean withHidden) throws IOException;

    boolean hasData();
    DataAccess data();

    boolean hasProps() throws IOException;
    JSONProps props() throws IOException;

    boolean delete(DeleteOption... options) throws IOException;

    Optional<File> toLocalFile() throws IOException;
    Optional<Path> toFilePath();
    Optional<URL> toURL();
    Optional<URI> toURI();

    boolean isModifiable();
    boolean isLockable();

    Resource resolve(String path) throws IOException;

    enum ReadOption {
        LOCK
    }

    enum WriteOption {
        LOCK,
        APPEND,
        ATOMIC,
        SYNC_DATA,
        SYNC_DATA_AND_META
    }

    enum DeleteOption {
        INCLUDE_CHILDREN,
        IGNORE_EXCEPTIONS
    }
}
