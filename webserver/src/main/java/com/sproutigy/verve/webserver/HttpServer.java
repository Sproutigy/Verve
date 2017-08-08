package com.sproutigy.verve.webserver;

import com.sproutigy.commons.async.Promise;
import com.sproutigy.verve.webserver.impl.HttpRequestContextImpl;
import com.sproutigy.verve.webserver.vertx.VertxHttpRequest;
import com.sproutigy.verve.webserver.vertx.VertxHttpResponse;
import com.sproutigy.verve.webserver.vertx.VertxHttpServer;
import io.vertx.core.Vertx;
import lombok.Getter;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;

public class HttpServer implements AutoCloseable {

    public static final String PORT_PROPERTY = "verve.httpserver.port";
    public static final String CONTEXT_PATH_PROPERTY = "verve.httpserver.contextpath";

    public static final int DEFAULT_PORT = 80;

    private Iterable<HttpRoute> routeProvider;

    public Iterable<HttpRoute> getRouteProvider() {
        if (routeProvider == null) {
            synchronized (this) {
                while (routeProvider == null) {
                    try {
                        this.wait();
                    } catch (InterruptedException ignore) { }
                }
            }
        }
        return routeProvider;
    }

    public void setRouteProvider(Iterable<HttpRoute> routeProvider) {
        synchronized (this) {
            this.routeProvider = routeProvider;
            this.notifyAll();
        }
    }

    @Getter
    private VertxHttpServer vertxHttpServer;

    @Getter
    private String contextPath = "/";

    public HttpServer() {
        this.vertxHttpServer = new VertxHttpServer();
    }

    public HttpServer(Vertx vertx) {
        this.vertxHttpServer = new VertxHttpServer(vertx);
    }

    public HttpServer(VertxHttpServer vertxHttpServer) {
        this.vertxHttpServer = vertxHttpServer;
    }

    public Promise<Void> start() {
        return start(null, null);
    }

    public Promise<Void> start(Integer port) {
        return start(port, null);
    }

    public Promise<Void> start(String contextPath) {
        return start(null, contextPath);
    }

    public Promise<Void> start(Integer port, String contextPath) {
        if (port != null) {
            getVertxHttpServer().setPort(port);
        }
        else {
            String s = System.getProperty(PORT_PROPERTY);
            if (s != null) {
                getVertxHttpServer().setPort(Integer.parseInt(s));
            }
        }

        this.contextPath = contextPath != null ? contextPath : System.getProperty(CONTEXT_PATH_PROPERTY);
        if (this.contextPath == null) this.contextPath = "/";

        getVertxHttpServer().setHandler(vertxRequest -> {
            HttpRequest req = new VertxHttpRequest(vertxRequest);
            HttpResponse resp = new VertxHttpResponse(vertxRequest);
            dispatch(req, resp);
        });

        return getVertxHttpServer().start();
    }

    public HttpRequestContext dispatch(HttpRequest request, HttpResponse response) {
        List<HttpRoute> routes = new LinkedList<>();
        for (HttpRoute route : getRouteProvider()) {
            routes.add(route);
        }
        routes.sort(Comparator.comparingInt(HttpRoute::getOrder));

        HttpRequestContextImpl ctx = new HttpRequestContextImpl(this, getContextPath(), request, response, routes.iterator());
        ctx.run();
        return ctx;
    }

    public Future<Void> stop() {
        return getVertxHttpServer().stop();
    }

    public void awaitStop() throws Exception {
        getVertxHttpServer().awaitStop();
    }

    @Override
    public void close() throws Exception {
        vertxHttpServer.close();
    }
}
