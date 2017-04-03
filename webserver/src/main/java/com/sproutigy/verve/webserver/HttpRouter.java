package com.sproutigy.verve.webserver;

import io.netty.handler.codec.http.HttpMethod;

import java.util.List;
import java.util.function.Function;

public interface HttpRouter extends Iterable<HttpRoute> {
    void add(HttpRoute route);
    void addAll(Iterable<HttpRoute> routes);
    List<HttpRoute> add(HttpHandler handler);
    List<HttpRoute> add(HttpObjectHandler handler);
    HttpRoute add(HttpRouteFilter filter, HttpHandler handler);
    HttpRoute add(int order, HttpRouteFilter filter, HttpHandler handler);
    HttpRoute add(HttpMethod method, String pathPattern, HttpHandler handler);
    HttpRoute add(String method, String pathPattern, HttpHandler handler);
    HttpRoute add(int order, String method, String pathPattern, HttpHandler handler);
    HttpRoute add(String pathPattern, HttpHandler handler);
    HttpRoute addRPC(String path, Object serviceInstance);
    HttpRoute addRedirect(String pathPattern, String redirect);
    HttpRoute addRedirect(String pathPattern, Function<HttpRequestContext, String> redirect);
    void remove(HttpRoute route);
}
