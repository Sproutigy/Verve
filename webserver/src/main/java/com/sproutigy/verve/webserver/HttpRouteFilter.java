package com.sproutigy.verve.webserver;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public interface HttpRouteFilter {
    /**
     *
     * @param req
     * @return Map of parameters (could be empty) when request matches route or null when not matches.
     */
    Map<String, String> filter(HttpRequest req);

    HttpRouteFilter ALWAYS = req -> Collections.emptyMap();

    static HttpRouteFilter always() {
        return ALWAYS;
    }

    static HttpRouteFilter of(String pathPattern) {
        return new DefaultHttpRouteFilter(pathPattern);
    }

    static HttpRouteFilter of(String method, String pathPattern) {
        return new DefaultHttpRouteFilter(method, pathPattern);
    }

    static HttpRouteFilter of(Collection<String> pathPatterns) {
        return new DefaultHttpRouteFilter(pathPatterns);
    }

    static HttpRouteFilter of(String method, Collection<String> pathPatterns) {
        return new DefaultHttpRouteFilter(method, pathPatterns);
    }

    static HttpRouteFilter of(Collection<String> methods, String pathPattern) {
        return new DefaultHttpRouteFilter(methods, pathPattern);
    }

    static HttpRouteFilter of(Collection<String> methods, Collection<String> pathPatterns) {
        return new DefaultHttpRouteFilter(methods, pathPatterns);
    }
}
