package com.sproutigy.verve.resources.tree;

import com.sproutigy.verve.resources.PathUtil;
import com.sproutigy.verve.resources.Resource;
import com.sproutigy.verve.resources.ResourceDecorator;
import com.sproutigy.verve.resources.exceptions.InvalidResolvePathResourceException;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Optional;

public class ResourceTreeNode extends ResourceDecorator {

    private String path;
    private ResourceTree tree;

    public ResourceTreeNode(ResourceTree tree, String path, Resource resource) {
        super(resource);
        this.path = PathUtil.normalize(path);
        this.tree = tree;
    }

    @Override
    public String getName() {
        return PathUtil.getName(path);
    }

    public Resource getResource() {
        return super.resource;
    }

    public ResourceTree getTree() {
        return tree;
    }

    public String getPath() {
        return path;
    }

    @Override
    public boolean hasParent() throws IOException {
        return getParent().isPresent();
    }

    @Override
    public Optional<? extends ResourceTreeNode> getParent() throws IOException {
        if (getPath().equals("/")) {
            return Optional.empty();
        }

        return Optional.ofNullable(resolve(PathUtil.getParentPath(getPath())));
    }

    @Override
    public ResourceTreeNode child(String name) throws IOException {
        return resolve(name);
    }

    @Override
    public Iterable<? extends ResourceTreeNode> getChildren() throws IOException {
        return getChildren(false);
    }

    @Override
    public Iterable<? extends ResourceTreeNode> getChildren(boolean withHidden) throws IOException {
        Collection<ResourceTreeNode> children = new LinkedList<>();
        for (Resource resource : super.getChildren()) {
            children.add(wrapInternal(resource, resource.getName()));
        }
        return children;
    }

    @Override
    public ResourceTreeNode resolve(String path) throws IOException {
        if (path == null) {
            throw new NullPointerException("path == null");
        }
        if (path.isEmpty() || path.equals(".") || path.equals("./")) {
            return this;
        }

        if (Objects.equals(path, getPath())) {
            return this;
        }

        if (path.startsWith("/")) {
            return getTree().resolve(path);
        }
        if (path.startsWith("./")) {
            return resolve(path.substring(2));
        }
        if (path.startsWith("../")) {
            if (!hasParent()) {
                throw new InvalidResolvePathResourceException();
            }
            return getParent().get().resolve(PathUtil.getParentPath(path.substring(3)));
        }

        return getTree().resolve(PathUtil.join(getPath(), path));
    }

    ResourceTreeNode resolveInternal(String path) throws IOException {
        return wrapInternal(getResource().resolve(path), path);
    }

    protected ResourceTreeNode wrapInternal(Resource resource, String subpath) {
        return new ResourceTreeNode(getTree(), PathUtil.join(getPath(), subpath), resource);
    }

    @Override
    public String toString() {
        String descriptor = getDescriptor();
        if (descriptor == null || descriptor.isEmpty()) {
            return getPath();
        }
        return getPath() + " : " + getDescriptor();
    }
}
