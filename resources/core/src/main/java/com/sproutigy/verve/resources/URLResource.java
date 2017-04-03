package com.sproutigy.verve.resources;

import com.sproutigy.verve.resources.exceptions.ChildrenNotAvailableResourceException;
import com.sproutigy.verve.resources.exceptions.InvalidResolvePathResourceException;

import java.io.*;
import java.net.*;
import java.util.Optional;

public class URLResource extends AbstractResource {
    protected URL url;

    public final static boolean USE_SOURCE_RESOURCE;

    static {
        USE_SOURCE_RESOURCE = Boolean.parseBoolean(System.getProperty("resource.source", "true"));
    }

    public URLResource(URL url) throws InvalidResolvePathResourceException {
        this.url = url;
        if (url == null) {
            throw new InvalidResolvePathResourceException();
        }
    }

    @Override
    public String getName() {
        String f = url.getFile();
        if (f.indexOf('/') > -1) {
            f = f.substring(f.lastIndexOf('/')+1);
        }
        return f;
    }

    @Override
    public String getDescriptor() {
        return url.toExternalForm();
    }

    @Override
    public Optional<Resource> getParent() {
        if (!url.getPath().isEmpty() && !url.getPath().equals("/")) {
            try {
                URI parentURI = url.getPath().endsWith("/") ? url.toURI().resolve("..") : url.toURI().resolve(".");
                return Optional.of(new URLResource(parentURI.toURL()));
            } catch (InvalidResolvePathResourceException | URISyntaxException | MalformedURLException | IllegalArgumentException ignore) {
            }
        }

        String path = url.getPath();
        if (path.endsWith("/")) {
            path = path.substring(0, path.length()-1);
        }
        if (path.endsWith("!")) {
            return Optional.empty();
        }

        int idx = path.lastIndexOf('/');
        if (idx > -1) {
            path = path.substring(0, idx+1);
            try {
                URL parentURL = new URL(url.getProtocol(), url.getHost(), url.getPort(), path);
                return Optional.of(new URLResource(parentURL));
            } catch (InvalidResolvePathResourceException | MalformedURLException ignore) { }
        }

        return Optional.empty();
    }

    @Override
    public boolean exists() throws IOException {
        Optional<File> localFile = toLocalFile();
        if (localFile.isPresent()) {
            return localFile.get().exists();
        }
        try {
            InputStream inputStream = url.openStream();
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception ignore) {
                }
                return true;
            }
        } catch (FileNotFoundException ignore) { }
        return false;
    }

    @Override
    public boolean isFile() {
        return true; //TODO!!
    }

    @Override
    public InputStream getInputStream(ReadOption... options) throws IOException {
        return url.openStream();
    }

    @Override
    public OutputStream getOutputStream(WriteOption... options) throws IOException {
        URLConnection connection = url.openConnection();
        connection.setDoOutput(true);
        return connection.getOutputStream();
    }

    @Override
    public Iterable<Resource> getChildren() throws IOException {
        Optional<File> localFile = toLocalFile();
        if (localFile.isPresent()) {
            return new FileResource(localFile.get()).getChildren();
        }
        throw new ChildrenNotAvailableResourceException();
    }

    @Override
    public Optional<File> toLocalFile() throws IOException {
        if (url.getProtocol().equals("file")) {
            File f = null;

            try {
                f = new File(url.toURI());
            } catch(URISyntaxException e) {
                try {
                    f = new File(url.getPath());
                } catch(Exception ignore) { }
            }

            if (f != null && USE_SOURCE_RESOURCE) {
                String path = f.getPath();
                path = path.replace("\\", "/");
                if (path.contains("/target/classes/")) {
                    path = path.replace("/target/classes/", "/src/main/resources/");
                }

                File src = new File(path);
                if (src.exists()) {
                    return Optional.of(src);
                }
            }
            return Optional.ofNullable(f);
        }
        return super.toLocalFile();
    }

    @Override
    public Optional<URL> toURL() {
        return Optional.of(url);
    }

    @Override
    public Optional<URI> toURI() {
        try {
            return Optional.of(url.toURI());
        } catch (URISyntaxException e) {
            return Optional.empty();
        }
    }

    @Override
    protected Resource resolveChild(String name) throws IOException {
        return new URLResource(new URL(url, name));
    }

    @Override
    public Resource resolve(String path) throws IOException {
        return new URLResource(new URL(url, path));
    }
}
