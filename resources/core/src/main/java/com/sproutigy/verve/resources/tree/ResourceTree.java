package com.sproutigy.verve.resources.tree;

import java.io.IOException;

public interface ResourceTree {
    ResourceTreeNode getRoot() throws IOException;
    ResourceTreeNode resolve(String path) throws IOException;
}
