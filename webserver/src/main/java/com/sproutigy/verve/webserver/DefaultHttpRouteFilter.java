package com.sproutigy.verve.webserver;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@RequiredArgsConstructor
public class DefaultHttpRouteFilter implements HttpRouteFilter {
    @Getter
    private Collection<String> methods;

    @Getter
    private Collection<PathPattern> pathPatterns;

    public DefaultHttpRouteFilter(String pathPattern) {
        this((String)null, pathPattern);
    }

    public DefaultHttpRouteFilter(Collection<String> pathPatterns) {
        this((String)null, pathPatterns);
    }

    public DefaultHttpRouteFilter(String method, String pathPattern) {
        if (method != null) {
            this.methods = Collections.singleton(method);
        } else {
            this.methods = Collections.emptyList();
        }
        if (pathPattern != null) {
            this.pathPatterns = Collections.singleton(new PathPattern(pathPattern));
        } else {
            this.pathPatterns = Collections.emptyList();
        }
    }

    public DefaultHttpRouteFilter(Collection<String> methods, Collection<String> pathPatterns) {
        this.methods = methods;
        this.pathPatterns = pathPatterns.stream().map(PathPattern::new).collect(Collectors.toList());
    }

    public DefaultHttpRouteFilter(String method, Collection<String> pathPatterns) {
        this(Collections.singleton(method), pathPatterns);
    }

    public DefaultHttpRouteFilter(Collection<String> methods, String pathPattern) {
        this(methods, Collections.singleton(pathPattern));
    }

    @Override
    public Map<String, String> filter(HttpRequest req) {
        if (methods == null || methods.size() == 0 || methods.contains(req.getMethod())) {
            if (pathPatterns != null && pathPatterns.size() > 0) {
                for (PathPattern pathPattern : pathPatterns) {
                    return pathPattern.matchParams(req.getContextualPath());
                }
            } else {
                return Collections.emptyMap();
            }
        }
        return null;
    }

}
