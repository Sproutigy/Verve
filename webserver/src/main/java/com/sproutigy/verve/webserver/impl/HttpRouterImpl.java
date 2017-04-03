package com.sproutigy.verve.webserver.impl;

import com.sproutigy.verve.webserver.*;
import com.sproutigy.verve.webserver.handlers.JSONRPCHttpHandler;
import com.sproutigy.verve.webserver.handlers.RedirectHttpHandler;
import io.netty.handler.codec.http.HttpMethod;

import java.util.*;
import java.util.function.Function;

public class HttpRouterImpl implements HttpRouter {
    private final LinkedList<HttpRoute> entries = new LinkedList<>();

    @Override
    public void add(HttpRoute entry) {
        int i;
        for (i = 0; i < entries.size(); i++) {
            HttpRoute route = entries.get(i);
            if (entry.getOrder() > route.getOrder()) {
                break;
            }
        }
        entries.add(i, entry);
    }

    @Override
    public void addAll(Iterable<HttpRoute> routes) {
        for (HttpRoute route : routes) {
            entries.add(route);
        }
    }

    @Override
    public List<HttpRoute> add(HttpHandler handler) {
        List<HttpRoute> routes = HttpRoute.from(handler);
        entries.addAll(routes);
        return routes;
    }

    @Override
    public List<HttpRoute> add(HttpObjectHandler handler) {
        List<HttpRoute> routes = HttpRoute.from(handler);
        entries.addAll(routes);
        return routes;
    }

    @Override
    public HttpRoute add(HttpRouteFilter filter, HttpHandler handler) {
        HttpRoute entry = new HttpRoute(filter, handler);
        add(entry);
        return entry;
    }

    @Override
    public HttpRoute add(int order, HttpRouteFilter filter, HttpHandler handler) {
        HttpRoute entry = new HttpRoute(order, filter, handler);
        add(entry);
        return entry;
    }

    @Override
    public HttpRoute add(HttpMethod method, String pathPattern, HttpHandler handler) {
        return add(method.toString(), pathPattern, handler);
    }

    @Override
    public HttpRoute add(String method, String pathPattern, HttpHandler handler) {
        return add(new DefaultHttpRouteFilter(method, pathPattern), handler);
    }

    @Override
    public HttpRoute add(int order, String method, String pathPattern, HttpHandler handler) {
        return add(order, new DefaultHttpRouteFilter(method, pathPattern), handler);
    }

    @Override
    public HttpRoute add(String pathPattern, HttpHandler handler) {
        return add(new DefaultHttpRouteFilter(pathPattern), handler);
    }

    @Override
    public HttpRoute addRPC(String path, Object serviceInstance) {
        return add(req -> {
            boolean ok = false;
            if (req.getPath().equals(path) || req.getPath().startsWith(path + "/")) {
                String m = req.getMethod();
                if (m.equals("POST")) {
                    ok = true;
                }
                if (m.equals("GET")) {
                    ok = HttpUtil.isLocalHost(req);
                }
            }
            return ok ? Collections.emptyMap() : null;
        }, new JSONRPCHttpHandler(serviceInstance));
    }

    @Override
    public HttpRoute addRedirect(String pathPattern, String redirect) {
        return add(pathPattern, new RedirectHttpHandler(x -> redirect));
    }

    @Override
    public HttpRoute addRedirect(String pathPattern, Function<HttpRequestContext, String> redirect) {
        return add(pathPattern, new RedirectHttpHandler(redirect));
    }

    @Override
    public void remove(HttpRoute route) {
        entries.remove(route);
    }

    @Override
    public Iterator<HttpRoute> iterator() {
        return entries.iterator();
    }
}
