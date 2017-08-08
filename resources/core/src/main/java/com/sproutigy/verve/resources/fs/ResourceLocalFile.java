package com.sproutigy.verve.resources.fs;

import com.sproutigy.verve.resources.PathUtil;
import com.sproutigy.verve.resources.Resource;

import java.io.*;
import java.nio.file.Files;
import java.util.Optional;

public class ResourceLocalFile implements Closeable {
    private Resource resource;
    private File tempFile;

    public ResourceLocalFile(Resource resource) {
        this.resource = resource;
    }

    public File provideLocalFile() throws IOException {
        if (tempFile != null) {
            return tempFile;
        }

        Optional<File> optFile = resource.toLocalFile();
        if (optFile.isPresent()) {
            return optFile.get();
        }
        else {
            synchronized (this) {
                if (tempFile == null) {
                    String prefix = PathUtil.getNameWithoutExtension(resource.getName());
                    String suffix = PathUtil.getExtension(resource.getName());
                    tempFile = Files.createTempFile(prefix, suffix).toFile();
                    tempFile.deleteOnExit();

                    try (InputStream stream = resource.data().input()) {
                        FileOutputStream out = new FileOutputStream(tempFile);
                        try {
                            byte[] temp = new byte[32768];
                            int rc;
                            while ((rc = stream.read(temp)) > 0)
                                out.write(temp, 0, rc);
                        } finally {
                            out.close();
                        }
                    }
                }
            }
        }

        return tempFile;
    }

    public boolean isLoadedAsTemporary() {
        return tempFile != null;
    }

    public InputStream openStream() throws IOException {
        if (tempFile != null && tempFile.exists()) {
            return new FileInputStream(tempFile);
        } else {
            return resource.data().input();
        }
    }

    public void invalidate() {
        if (tempFile != null) {
            tempFile.delete();
            tempFile = null;
        }
    }

    @Override
    public void close() {
        invalidate();
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            close();
        } catch (Throwable ignore) {
        }

        super.finalize();
    }
}
