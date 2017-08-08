package com.sproutigy.verve.resources;

import com.sproutigy.verve.resources.exceptions.InvalidResolvePathResourceException;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClassPathResource extends URLResource {
    private ClassLoader classLoader;
    private Class clazz;

    private String path;

    public ClassPathResource(String path) throws InvalidResolvePathResourceException {
        this(path, Thread.currentThread().getContextClassLoader());
    }

    public ClassPathResource(String path, ClassLoader classLoader) throws InvalidResolvePathResourceException {
        this(path, classLoader, null);
    }

    public ClassPathResource(String path, Class clazz) throws InvalidResolvePathResourceException {
        this(path, null, clazz);
    }

    public ClassPathResource(String path, ClassLoader classLoader, Class clazz) throws InvalidResolvePathResourceException {
        super(clazz != null ? clazz.getResource(path) : classLoader.getResource(path));
        this.clazz = clazz;
        this.classLoader = classLoader != null ? classLoader : clazz.getClassLoader();
        this.path = path;
    }

    @Override
    public String getDescriptor() {
        return url.toExternalForm();
    }

    @Override
    public Optional<Resource> getParent() {
        String parentPath = PathUtil.getParentPath(path);
        try {
            return Optional.of(new ClassPathResource(parentPath, classLoader, clazz));
        } catch (InvalidResolvePathResourceException e) {
            return Optional.empty();
        }
    }

    @Override
    public Iterable<Resource> getChildren() throws IOException {
        Collection<Resource> ret = new LinkedHashSet<>();
        try {
            String[] children = getResourceListing();
            if (children != null) {
                Resource childResource;
                for (String child : children) {
                    childResource = new ClassPathResource(PathUtil.join(path,  child), classLoader, clazz);
                    ret.add(childResource);
                }
            }
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
        return Collections.unmodifiableCollection(ret);
    }

    private String[] getResourceListing() throws URISyntaxException, IOException {
        URL dirURL;
        if (clazz != null) {
            dirURL = clazz.getResource(path);
        } else {
            dirURL = classLoader.getResource(path);
        }

        if (dirURL != null && dirURL.getProtocol().equals("file")) {
            return new File(dirURL.toURI()).list();
        }

        if (dirURL == null) {
            if (clazz != null) {
                String me = clazz.getName().replace(".", "/") + ".class";
                dirURL = clazz.getClassLoader().getResource(me);
            }
        }

        if (dirURL != null && dirURL.getProtocol().equals("jar")) {
            String jarPath = dirURL.getPath().substring(5, dirURL.getPath().indexOf("!")); //strip out only the JAR file
            JarFile jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"));
            Enumeration<JarEntry> entries = jar.entries();
            Set<String> result = new LinkedHashSet<>();
            while(entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.startsWith(path) && name.length() > path.length()) {
                    String entry = name.substring(path.length());
                    int idx = entry.indexOf('/');
                    if (idx >= 0) {
                        entry = entry.substring(0, idx);
                    }
                    result.add(entry);
                }
            }
            return result.toArray(new String[result.size()]);
        }

        throw new UnsupportedOperationException("Cannot list files for URL "+dirURL);
    }

}
