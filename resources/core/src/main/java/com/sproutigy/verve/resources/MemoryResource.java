package com.sproutigy.verve.resources;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public class MemoryResource extends AbstractResource {
    private String name;
    private String descriptor;
    private Resource parent;
    private List<Resource> children = new CopyOnWriteArrayList<>();

    public MemoryResource(String name) {
        this.name = name;
        this.descriptor = name;
    }

    public MemoryResource(String name, String descriptor) {
        this.name = name;
        this.descriptor = descriptor;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescriptor() {
        return descriptor;
    }

    @Override
    public boolean exists() throws IOException {
        return true;
    }

    @Override
    public boolean isFile() {
        return true;
    }

    @Override
    public boolean isContainer() {
        return true;
    }

    @Override
    public Optional<Resource> getParent() {
        return Optional.ofNullable(parent);
    }

    public void setParent(Resource parent) {
        this.parent = parent;
    }

    @Override
    public List<Resource> getChildren() throws IOException {
        return children;
    }

    @Override
    public Iterable<? extends Resource> getChildren(boolean withHidden) throws IOException {
        return children;
    }

    public void setChildren(List<Resource> children) {
        this.children = children;
    }

}
