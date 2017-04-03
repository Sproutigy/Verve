package com.sproutigy.verve.resources;

public final class PathUtil {
    private PathUtil() { }

    private static final char ALT_SEPARATOR = '\\';
    public static final char SEPARATOR = '/';

    public static final String ROOT = "/";

    public static String normalize(String path) {
        String ret = path.replace(ALT_SEPARATOR, SEPARATOR);
        if (ret.endsWith(SEPARATOR+"")) {
            ret = ret.substring(0, ret.length()-1);
        }
        return ret;
    }

    public static String join(String... elements) {
        StringBuilder builder = new StringBuilder();
        for (String element : elements) {
            element = element.replace(ALT_SEPARATOR, SEPARATOR);
            if (builder.length() > 0) {
                boolean endsWithSeparator = (builder.lastIndexOf("" + SEPARATOR) == builder.length() - 1);
                if (endsWithSeparator && element.startsWith("/")) {
                    builder.append(element.substring(1));
                } else {
                    builder.append(element);
                }
            }
            else {
                builder.append(element);
            }
        }
        return normalize(builder.toString());
    }


    public static String getParentPath(String path) {
        int idx;
        if (path.endsWith("/") || path.endsWith("\\")) {
            path = path.substring(0, path.length()-1);
        }
        if (path.equals("/") || path.equals("\\")) {
            return "";
        }

        String ret = "";

        idx = path.lastIndexOf('/');
        if (idx > -1) {
            ret = path.substring(0, idx);
            if (ret.isEmpty()) {
                ret = "/";
            }
        } else {
            idx = path.lastIndexOf('\\');
            if (idx > -1) {
                ret = path.substring(0, idx);
                if (ret.isEmpty()) {
                    ret = "\\";
                }
            }
        }

        return ret;
    }

    public static String getName(String path) {
        String filename = normalize(path);
        int idx;
        idx = filename.lastIndexOf('/');
        if (idx > -1) {
            filename = filename.substring(idx+1);
        }
        idx = filename.lastIndexOf('\\');
        if (idx > -1) {
            filename = filename.substring(idx+1);
        }
        idx = filename.lastIndexOf('?');
        if (idx > -1) {
            filename = filename.substring(0, idx);
        }
        return filename;
    }

    public static String getExtension(String path) {
        String filename = getName(path);
        int idx = filename.lastIndexOf('.');
        if (idx > 0) {
            return filename.substring(idx+1);
        }
        return "";
    }

    public static String getNameWithoutExtension(String path) {
        String filename = getName(path);
        return filename.substring(0, filename.length() - getExtension(path).length());
    }
}
