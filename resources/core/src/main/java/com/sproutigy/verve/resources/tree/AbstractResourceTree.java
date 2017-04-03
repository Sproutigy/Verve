package com.sproutigy.verve.resources.tree;

import java.io.IOException;

public abstract class AbstractResourceTree implements ResourceTree {
    @Override
    public ResourceTreeNode resolve(String path) throws IOException {
        if (path == null) {
            throw new NullPointerException("path == null");
        }
        if (!path.startsWith("/")) {
            throw new IllegalArgumentException("Path not absolute");
        }

        if (path.equals("/")) {
            return getRoot();
        }

        return resolveInternal(path);
    }

    protected abstract ResourceTreeNode resolveInternal(String path) throws IOException;
}
