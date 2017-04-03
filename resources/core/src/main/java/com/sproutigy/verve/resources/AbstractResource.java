package com.sproutigy.verve.resources;

import com.sproutigy.commons.binary.Binary;
import com.sproutigy.verve.resources.exceptions.InvalidResolvePathResourceException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public abstract class AbstractResource implements Resource {

    protected final Executor executor = Executors.newSingleThreadExecutor();

    public AbstractResource() {
    }

    @Override
    public int hashCode() {
        if (getDescriptor() != null) {
            return getDescriptor().hashCode();
        } else {
            return super.hashCode();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Resource) {
            Resource other = (Resource)obj;
            if (this.getDescriptor() != null) {
                return Objects.equals(this.getDescriptor(), other.getDescriptor());
            }
        }
        return super.equals(obj);
    }

    @Override
    public boolean hasParent() {
        return getParent().isPresent();
    }

    @Override
    public Optional<? extends Resource> getParent() {
        return Optional.empty();
    }

    @Override
    public Resource getRoot() throws IOException {
        Resource cur = this;
        while (true) {
            Optional<? extends Resource> optParent = cur.getParent();
            if (optParent.isPresent()) {
                if (optParent.get() != cur) {
                    cur = optParent.get();
                }
                else {
                    break;
                }
            } else {
                break;
            }
        }
        return cur;
    }

    @Override
    public boolean create() throws IOException {
        throw new IOException("Resource could not be created");
    }

    @Override
    public boolean isContainer() {
        return hasChildren();
    }

    @Override
    public boolean hasChildren() {
        return iterator().hasNext();
    }

    @Override
    public boolean hasChild(String name) {
        for (Resource resource : this) {
            if (Objects.equals(resource.getName(), name)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public final Resource child(String name) throws IOException {
        if (name.contains("/") || name.contains("\\")) {
            throw new IllegalArgumentException("Child name cannot contain special path characters");
        }
        return resolveChild(name);
    }

    @Override
    public OutputStream getOutputStream(WriteOption... options) throws IOException {
        throw new IOException("Resource is unmodifiable");
    }

    @Override
    public boolean hasData() {
        return isFile();
    }

    @Override
    public Binary getData() throws IOException {
        try (InputStream inputStream = getInputStream(ReadOption.LOCK)) {
            return Binary.from(inputStream);
        }
    }

    @Override
    public void setData(Binary binary) throws IOException {
        try (OutputStream outputStream = getOutputStream(WriteOption.ATOMIC, WriteOption.LOCK, WriteOption.SYNC_DATA_AND_META)) {
            binary.toStream(outputStream);
        }
    }

    @Override
    public boolean delete(DeleteOption... options) throws IOException {
        throw new IOException("Resource is unmodifiable");
    }

    @Override
    public Optional<File> toLocalFile() throws IOException {
        return Optional.empty();
    }

    @Override
    public Optional<Path> toFilePath() {
        Optional<URI> optURI = toURI();
        if (optURI.isPresent()) {
            try {
                return Optional.of(Paths.get(optURI.get()));
            } catch(Exception ignore) { }
        }
        return Optional.empty();
    }

    @Override
    public Optional<URL> toURL() {
        return Optional.empty();
    }

    @Override
    public Optional<URI> toURI() {
        return Optional.empty();
    }

    @Override
    public boolean isModifiable() {
        return false;
    }

    @Override
    public boolean isLockable() {
        return false;
    }

    protected void ensureExistence() throws IOException {
        if (!exists()) {
            throw new IllegalStateException("Resource not exists");
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Iterator<Resource> iterator() {
        try {
            return (Iterator<Resource>)getChildren().iterator();
        } catch (IOException e) {
            throw new RuntimeException("Could not iterate children resources", e);
        }
    }

    @Override
    public Resource resolve(String path) throws IOException {
        if (path == null || path.isEmpty() || path.equals(".")) {
            return this;
        }

        if (path.indexOf('\\') > 0) {
            path = path.replace("\\", "/");
        }

        if (path.charAt(0) == PATH_SEPARATOR) {
            return getRoot().resolve(path.substring(1));
        }

        if (path.startsWith("./")) {
            if (!hasParent()) {
                throw new InvalidResolvePathResourceException();
            }
            return resolve(path.substring(2));
        }

        if (path.startsWith("../")) {
            if (hasParent()) {
                throw new InvalidResolvePathResourceException();
            }
            return getParent().get().resolve(path.substring(3));
        }

        if (path.indexOf(PATH_SEPARATOR) > -1) {
            Resource resource = child(path.substring(0, path.indexOf(PATH_SEPARATOR)));
            return resource.resolve(path.substring(path.indexOf(PATH_SEPARATOR)+1));
        }
        else {
            return child(path);
        }
    }

    protected Resource resolveChild(String name) throws IOException {
        throw new IOException("Could not resolve child resource");
    }

    @Override
    public String toString() {
        String descriptor = getDescriptor();
        if (descriptor == null || descriptor.isEmpty()) {
            return super.toString();
        }
        return descriptor;
    }

    @Override
    public void close() throws Exception {

    }

}
