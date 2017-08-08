package com.sproutigy.verve.resources;

import com.sproutigy.verve.resources.fs.FSUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class FileResource extends AbstractResource {
    protected File file;

    public FileResource(File file) {
        this.file = file;
    }

    public FileResource(String localPath) {
        this(new File(localPath));
    }

    public FileResource(Path path) {
        this(path.toFile());
    }

    @Override
    public String getName() {
        return file.getName();
    }

    @Override
    public String getDescriptor() {
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            return file.getAbsolutePath();
        }
    }

    @Override
    public boolean exists() throws IOException {
        return file.exists();
    }

    @Override
    public Optional<Resource> getParent() {
        File parentFile = file.getParentFile();
        if (parentFile == null) {
            return Optional.empty();
        }
        return Optional.of(new FileResource(parentFile));
    }

    @Override
    public boolean create() throws IOException {
        return file.createNewFile();
    }

    @Override
    public boolean createContainer() throws IOException {
        return file.mkdirs();
    }

    @Override
    public boolean isContainer() {
        return file.isDirectory();
    }

    @Override
    public boolean hasChild(String name) {
        return new File(file, name).exists();
    }

    @Override
    public boolean hasData() {
        return isFile();
    }

    @Override
    public boolean isFile() {
        return file.isFile();
    }

    @Override
    protected Resource resolveChild(String name) {
        return resolveChild(new File(file, name));
    }

    protected Resource resolveChild(File childFile) {
        return new FileResource(childFile);
    }

    @Override
    public DataAccess data() {
        return new FileDataAccess(file);
    }

    @Override
    public boolean delete(DeleteOption... options) throws IOException {
        boolean ignoreExceptions = hasOption(options, DeleteOption.IGNORE_EXCEPTIONS);
        try {
            if (!hasOption(options, DeleteOption.INCLUDE_CHILDREN)) {
                return Files.deleteIfExists(file.toPath());
            } else {
                if (ignoreExceptions) {
                    return FSUtils.deleteWithChildrenIgnoreExceptions(file);
                } else {
                    return FSUtils.deleteWithChildren(file);
                }
            }
        } catch(IOException e) {
            if (ignoreExceptions) {
                return false;
            }
            throw e;
        }
    }

    @Override
    public Optional<File> toLocalFile() throws IOException {
        return Optional.of(file);
    }

    @Override
    public Optional<Path> toFilePath() {
        return Optional.of(file.toPath());
    }

    @Override
    public boolean isModifiable() {
        return Files.isWritable(file.toPath());
    }

    @Override
    public Iterable<Resource> getChildren(boolean withHidden) throws IOException {
        File[] childs = file.listFiles();
        if (childs == null) {
            return Collections.emptySet();
        }
        List<Resource> resources = new LinkedList<>();
        for (File child : childs) {
            if ((withHidden || !isHidden(child)) && !isSpecialChildName(child.getName())) {
                resources.add(resolveChild(child));
            }
        }
        return Collections.unmodifiableList(resources);
    }

    public static boolean isHidden(FileResource fileResource) {
        return isHidden(fileResource.file);
    }

    public static boolean isHidden(File file) {
        return file.getName().startsWith(".") || file.isHidden();
    }

    private boolean hasOption(Object[] options, Object option) {
        for(Object opt : options) {
            if (opt == option) {
                return true;
            }
        }
        return false;
    }
}
