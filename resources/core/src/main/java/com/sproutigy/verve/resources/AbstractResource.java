package com.sproutigy.verve.resources;

import com.sproutigy.verve.resources.exceptions.ChildrenNotAvailableResourceException;
import com.sproutigy.verve.resources.exceptions.InvalidResolvePathResourceException;
import com.sproutigy.verve.resources.props.DataAccessJSONProps;
import com.sproutigy.verve.resources.props.JSONProps;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public abstract class AbstractResource implements Resource {

    private static final String PROPS_FILENAME = ".props.json";

    protected final Executor executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
        private ThreadFactory parent = Executors.defaultThreadFactory();

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = parent.newThread(r);
            thread.setDaemon(true);
            return thread;
        }
    });

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
    public boolean createContainer() throws IOException {
        throw new IOException("Resource could not be created as a container");
    }

    @Override
    public boolean isContainer() {
        return hasChildren();
    }

    @Override
    public boolean hasChildren() {
        try {
            return iterator().hasNext();
        } catch (RuntimeException re) {
            if (re.getCause() instanceof ChildrenNotAvailableResourceException) {
                return false;
            }
            throw re;
        }
    }

    @Override
    public boolean hasChild(String name) throws IOException {
        for (Resource resource : getChildren(true)) {
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
    public boolean hasData() {
        return false;
    }

    @Override
    public DataAccess data() {
        return null; //TODO
    }

    @Override
    public boolean hasProps() throws IOException {
        if (!exists()) {
            return false;
        }

        if (isContainer()) {
            return hasChild(PROPS_FILENAME);
        } else {
            if (!hasParent()) {
                return false;
            }
            return getParent().get().hasChild(getName() + PROPS_FILENAME);
        }
    }

    @Override
    public JSONProps props() throws IOException {
        if (!exists()) {
            throw new IOException("Cannot fetch props of non-existing resource");
        }

        if (isContainer()) {
            return new DataAccessJSONProps(child(PROPS_FILENAME).data());
        } else {
            if (!hasParent()) {
                throw new IOException("No parent - cannot fetch props");
            }

            Resource propsResource = getParent().get().child(getName() + PROPS_FILENAME);
            return new DataAccessJSONProps(propsResource.data());
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
            try {
                return (Iterator<Resource>) getChildren().iterator();
            } catch (ChildrenNotAvailableResourceException noChildren) {
                return Collections.emptyIterator();
            }
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

    @Override
    public Iterable<? extends Resource> getChildren() throws IOException {
        return getChildren(false);
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

    protected boolean isSpecialChildName(String childName) {
        if (childName.endsWith(PROPS_FILENAME)) {
            return true;
        }
        if (childName.startsWith(".~")) {
            return true;
        }
        return false;
    }

}
