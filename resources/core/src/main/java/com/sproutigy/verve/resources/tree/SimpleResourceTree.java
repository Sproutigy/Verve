package com.sproutigy.verve.resources.tree;

import com.sproutigy.verve.resources.Resource;

import java.io.IOException;

public class SimpleResourceTree extends AbstractResourceTree {
    private ResourceTreeNode root;

    public SimpleResourceTree(Resource root) {
        this.root = new ResourceTreeNode(this, "/", root);
    }

    @Override
    public ResourceTreeNode getRoot() throws IOException {
        return root;
    }

    @Override
    protected ResourceTreeNode resolveInternal(String path) throws IOException {
        return getRoot().resolveInternal(path.substring(1));
    }
}
