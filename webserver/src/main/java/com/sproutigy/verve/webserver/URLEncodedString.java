package com.sproutigy.verve.webserver;

import lombok.SneakyThrows;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;

/**
 * Class that creates or parses query strings and POST application/x-www-form-urlencoded data.
 * Requires UTF-8 string encoding.
 */
public class URLEncodedString implements Iterable<Map.Entry<String, String>> {

    public static final String DEFAULT_CHARSET = "UTF-8";
    private String charset = DEFAULT_CHARSET;
    private String s = "";

    public URLEncodedString() {
    }

    public URLEncodedString(String s) {
        this(s, DEFAULT_CHARSET);
    }

    public URLEncodedString(String s, String charset) {
        if (s == null) {
            throw new NullPointerException("s == null");
        }
        if (charset == null) {
            throw new NullPointerException("charset == null");
        }

        this.s = s;
        this.charset = charset;
    }

    public URLEncodedString(Map<String, String> map) {
        addAll(map);
    }

    @Override
    public int hashCode() {
        return s.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof URLEncodedString) {
            URLEncodedString other = (URLEncodedString)obj;
            return other.toString().equals(this.toString());
        }
        return false;
    }

    @Override
    public String toString() {
        return s;
    }

    @SneakyThrows
    public URLEncodedString add(String name, String value) {
        if (name == null) {
            throw new NullPointerException("name == null");
        }
        if (name.isEmpty()) {
            throw new IllegalArgumentException("name is empty");
        }
        int bufsize = name.length() * 2;
        if (value != null) {
            bufsize += 1 + value.length() * 2;
        }
        StringBuilder builder = new StringBuilder(bufsize);
        if (!s.isEmpty()) {
            builder.append("&");
        }
        builder.append(URLEncoder.encode(name, charset));
        if (value != null) {
            builder.append("=");
            builder.append(URLEncoder.encode(value, charset));
        }
        s = s + builder.toString();
        return this;
    }

    public URLEncodedString addAll(String name, Collection<String> values) {
        for (String value : values) {
            add(name, value);
        }
        return this;
    }

    public URLEncodedString addAll(Map<String, String> map) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            add(entry.getKey(), entry.getValue());
        }
        return this;
    }

    public List<String> getAll(String name) {
        List<String> values = new LinkedList<>();
        for (Map.Entry<String, String> entry : this) {
            if (Objects.equals(entry.getKey(), name)) {
                values.add(entry.getValue());
            }
        }
        return values;
    }

    public String getFirst(String name) {
        for (Map.Entry<String, String> entry : this) {
            if (Objects.equals(entry.getKey(), name)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public String getLast(String name) {
        String value = null;
        for (Map.Entry<String, String> entry : this) {
            if (Objects.equals(entry.getKey(), name)) {
                value = entry.getValue();
            }
        }
        return value;
    }

    public Map<String, List<String>> toMultiMap() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : this) {
            List<String> list = map.computeIfAbsent(entry.getKey(), k -> new LinkedList<>());
            list.add(entry.getValue());
        }
        return map;
    }

    public Map<String, String> toSimpleMap() {
        Map<String, String> map = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : this) {
            map.put(entry.getKey(), entry.getValue());
        }
        return map;
    }

    public URLEncodedString clear() {
        s = "";
        return this;
    }

    @Override
    public Iterator<Map.Entry<String, String>> iterator() {
        return new Iterator<Map.Entry<String, String>>() {
            int i = 0;
            int from = 0;
            int to = -1;
            boolean fetchedNext = false;

            @Override
            public boolean hasNext() {
                if (fetchedNext) {
                    return true;
                }
                if (from >= s.length()) {
                    return false;
                }

                int next = s.indexOf("&", from);
                if (next == -1) {
                    to = s.length();
                } else {
                    to = next;
                }

                fetchedNext = true;
                return true;
            }

            @SneakyThrows
            @Override
            public Map.Entry<String, String> next() {
                if (!fetchedNext) {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                }
                String subs = s.substring(from, to);
                int assignIndex = subs.indexOf("=");

                String key;
                String value = null;
                if (assignIndex > -1) {
                    key = subs.substring(0, assignIndex);
                    value = subs.substring(assignIndex + 1);
                } else {
                    key = subs;
                }

                key = URLDecoder.decode(key, charset);
                if (value != null) {
                    value = URLDecoder.decode(value, charset);
                }

                Map.Entry<String, String> entry = new AbstractMap.SimpleImmutableEntry<String, String>(key, value);

                from = to + 1;
                to = -1;
                fetchedNext = false;

                return entry;
            }
        };
    }
}
