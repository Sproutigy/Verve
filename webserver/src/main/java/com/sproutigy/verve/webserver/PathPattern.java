package com.sproutigy.verve.webserver;

import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PathPattern {

    private final String pathPattern;
    private final Pattern regexPattern;
    private final Collection<String> paramNames;

    public PathPattern(String pathPattern) {
        this.pathPattern = pathPattern;
        this.regexPattern = Pattern.compile(transformToRegex(pathPattern));
        this.paramNames = Collections.unmodifiableSet(getGroupNames(regexPattern));
    }

    public PathPattern(Pattern regexPattern) {
        this.pathPattern = regexPattern.pattern();
        this.regexPattern = regexPattern;
        this.paramNames = Collections.unmodifiableSet(getGroupNames(regexPattern));
    }

    private static int indexOfAny(String s, String... substrs) {
        int ret = -1;
        for (String substr : substrs) {
            int i = s.indexOf(substr);
            if (i != -1 && (i < ret || ret == -1)) {
                ret = i;
            }
        }
        return ret;
    }

    public static String transformToRegex(String path) {
        String ret = path;

        int idx;
        while ((idx = indexOfAny(ret, "/:", "/*")) >= 0) {
            int ending = indexOfAfter(ret, idx + 2, "/");
            if (ending == -1) {
                ending = ret.length();
            }

            boolean greedy = ret.charAt(idx + 1) == '*';

            String paramName = ret.substring(idx + 2, ending);

            String regex;
            if (!paramName.isEmpty()) {
                regex = "/(?<" + paramName + ">" + (greedy ? ".*" : "[^/]+") + ")";
            }
            else {
                regex = "/(" + (greedy ? ".*" : "[^/]+") + ")";
            }
            ret = ret.substring(0, idx) + regex + ret.substring(ending);
        }

        if (ret.endsWith("/*")) {
            ret = ret.substring(0, ret.length() - 2) + "(.*)";
        }

        return ret;
    }

    public String getPathPattern() {
        return pathPattern;
    }

    public Pattern getRegexPattern() {
        return regexPattern;
    }

    public Collection<String> getParamNames() {
        return paramNames;
    }

    public Matcher matcher(String path) {
        return regexPattern.matcher(path);
    }

    public Map<String, String> matchParams(Matcher matcher) {
        return getNamedGroups(matcher, paramNames);
    }

    public boolean matches(String path) {
        return matcher(path).matches();
    }

    /**
     * Tries to match path with pattern when possible and returns matched parameters
     *
     * @param path
     * @return Map or null when path not matches pattern
     */
    public Map<String, String> matchParams(String path) {
        Matcher matcher = matcher(path);
        if (!matcher.matches()) {
            return null;
        }
        return matchParams(matcher);
    }

    @Override
    public String toString() {
        return pathPattern;
    }

    private static int indexOfAfter(String str, int position, String substr) {
        if (!str.substring(position).contains(substr)) {
            return -1;
        } else {
            return str.substring(position).indexOf(substr) + position;
        }
    }

    private static Method namedGroupsMethod = null;
    private static Pattern GROUPS_PATTERN = Pattern.compile("\\(\\?<([a-zA-Z][a-zA-Z0-9]*)>");

    static {
        try {
            namedGroupsMethod = Pattern.class.getDeclaredMethod("namedGroups");
            namedGroupsMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
        }
    }

    @SuppressWarnings("unchecked")
    private static Set<String> getGroupNames(Pattern pattern) {
        //try to access private method using generics
        if (namedGroupsMethod != null) {
            try {
                Map<String, Integer> namedGroups = (Map<String, Integer>) namedGroupsMethod.invoke(pattern);
                if (namedGroups != null) {
                    return namedGroups.keySet();
                }
            } catch (Exception ignore) {
            }
        }

        //match regex
        Set<String> namedGroups = new LinkedHashSet<>();

        Matcher m = GROUPS_PATTERN.matcher(pattern.pattern());

        while (m.find()) {
            namedGroups.add(m.group(1));
        }

        return namedGroups;
    }

    private static Map<String, String> getNamedGroups(Matcher matcher, Collection<String> groupsNames) {
        if (groupsNames.size() == 0) return Collections.emptyMap();

        Map<String, String> map = new LinkedHashMap<>();
        for (String g : groupsNames) {
            map.put(g, matcher.group(g));
        }
        return map;
    }

}