package com.sproutigy.verve.resources.tree;

import com.sproutigy.verve.resources.MemoryResource;
import com.sproutigy.verve.resources.Resource;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class MountableResourceTree extends AbstractResourceTree {
    private ResourceTreeNode root = new ResourceTreeNode(this, "/", new MemoryResource("", "/"));
    private ConcurrentHashMap<String, ResourceTreeNode> mountNodes = new ConcurrentHashMap<>();

    @Override
    public ResourceTreeNode getRoot() throws IOException {
        if (mountNodes.containsKey("/")) {
            return mountNodes.get("/");
        }

        return root;
    }

    public void mount(String path, Resource resource) throws IOException {
        mountNodes.put(path, new ResourceTreeNode(this, path, resource));
    }

    public void unmount(String path) {
        mountNodes.remove(path);
    }

    @Override
    protected ResourceTreeNode resolveInternal(String path) throws IOException {
        for (String mountPath : mountNodes.keySet()) {
            if (path.startsWith(mountPath)) {
                return mountNodes.get(path).resolveInternal(path.substring(mountPath.length()));
            }
        }
        return getRoot().resolveInternal(path.substring(1));
    }
}
