package com.sproutigy.verve.resources;

import com.sproutigy.commons.binary.Binary;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.Consumer;

public abstract class ResourceDecorator implements Resource {
    protected Resource resource;

    public ResourceDecorator(Resource resource) {
        this.resource = resource;
    }

    @Override
    public String getName() {
        return resource.getName();
    }

    @Override
    public String getDescriptor() {
        return resource.getDescriptor();
    }

    @Override
    public boolean hasParent() throws IOException {
        return resource.hasParent();
    }

    @Override
    public Optional<? extends Resource> getParent() throws IOException {
        return resource.getParent();
    }

    @Override
    public Resource getRoot() throws IOException {
        return resource.getRoot();
    }

    @Override
    public boolean create() throws IOException {
        return resource.create();
    }

    @Override
    public boolean exists() throws IOException {
        return resource.exists();
    }

    @Override
    public boolean isContainer() {
        return resource.isContainer();
    }

    @Override
    public boolean isFile() {
        return resource.isFile();
    }

    @Override
    public boolean hasChild(String name) {
        return resource.hasChild(name);
    }

    @Override
    public Resource child(String name) throws IOException {
        return resource.child(name);
    }

    @Override
    public boolean hasChildren() {
        return resource.hasChildren();
    }

    @Override
    public Iterable<? extends Resource> getChildren() throws IOException {
        return resource.getChildren();
    }

    @Override
    public InputStream getInputStream(ReadOption... options) throws IOException {
        return resource.getInputStream(options);
    }

    @Override
    public OutputStream getOutputStream(WriteOption... options) throws IOException {
        return resource.getOutputStream(options);
    }

    @Override
    public boolean hasData() {
        return resource.hasData();
    }

    @Override
    public Binary getData() throws IOException {
        return resource.getData();
    }

    @Override
    public void setData(Binary binary) throws IOException {
        resource.setData(binary);
    }

    @Override
    public boolean delete(DeleteOption... options) throws IOException {
        return resource.delete(options);
    }

    @Override
    public Optional<File> toLocalFile() throws IOException {
        return resource.toLocalFile();
    }

    @Override
    public Optional<Path> toFilePath() {
        return resource.toFilePath();
    }

    @Override
    public Optional<URL> toURL() {
        return resource.toURL();
    }

    @Override
    public Optional<URI> toURI() {
        return resource.toURI();
    }

    @Override
    public boolean isModifiable() {
        return resource.isModifiable();
    }

    @Override
    public boolean isLockable() {
        return resource.isLockable();
    }

    @Override
    public Iterator<Resource> iterator() {
        return resource.iterator();
    }

    @Override
    public void forEach(Consumer<? super Resource> action) {
        resource.forEach(action);
    }

    @Override
    public Spliterator<Resource> spliterator() {
        return resource.spliterator();
    }

    @Override
    public void close() throws Exception {
        resource.close();
    }

    @Override
    public Resource resolve(String path) throws IOException {
        return resource.resolve(path);
    }

}
