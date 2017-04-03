package com.sproutigy.verve.webserver.vertx;

import com.sproutigy.commons.async.Deferred;
import com.sproutigy.commons.async.Promise;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.concurrent.ExecutionException;

@Slf4j
public class VertxHttpServer implements AutoCloseable, Handler<HttpServerRequest> {

    static VertxHttpServer instance;

    @Getter
    private Vertx vertx;

    public VertxHttpServer() {
        this(VertxProvider.provide());
    }

    @Inject
    public VertxHttpServer(Vertx vertx) {
        this.vertx = vertx;
    }

    @Getter
    @Setter
    private HttpServerOptions serverOptions = defaultServerOptions();

    @Getter
    @Setter
    private DeploymentOptions verticleDeploymentOptions = defaultVerticleDeploymentOptions();

    @Getter
    @Setter
    private volatile String verticleDeploymentId;

    @Getter
    @Setter
    private transient Handler<HttpServerRequest> handler = null;

    private Deferred<Void> stopDeferred = null;
    private transient boolean stopped = false;

    public int getPort() {
        return serverOptions.getPort();
    }

    public void setPort(int port) {
        serverOptions.setPort(port);
    }

    private static HttpServerOptions defaultServerOptions() {
        HttpServerOptions options = new HttpServerOptions();
        options.setCompressionSupported(true);
        return options;
    }

    private static DeploymentOptions defaultVerticleDeploymentOptions() {
        DeploymentOptions options = new DeploymentOptions();
        int processors = Runtime.getRuntime().availableProcessors();
        options.setInstances(processors * 2);
        options.setWorker(true);
        options.setWorkerPoolSize(processors * 50);
        return options;
    }

    @Override
    public void handle(HttpServerRequest req) {
        if (log.isTraceEnabled()) {
            log.trace("Handling HTTP request: {}", VertxUtil.describeRequest(req));
        }

        req.exceptionHandler(e -> {
            log.error("Request {} error", VertxUtil.describeRequest(req), e);
            VertxUtil.endQuietly(req);
        });

        Handler<HttpServerRequest> handler = this.handler;
        if (handler != null) {
            try {
                handler.handle(req);
            } catch (Throwable e) {
                log.error("Request {} handler error", VertxUtil.describeRequest(req), e);
                VertxUtil.endQuietly(req);
            }
        } else {
            log.trace("HTTP request handler not set");
            VertxUtil.endQuietly(req, 404);
        }
    }

    @SuppressWarnings("unchecked")
    public Promise<Void> start() {
        Deferred<Void> startDeferred = Promise.defer();
        if (verticleDeploymentId == null) {
            log.info("Starting HTTP Server port {}...", getServerOptions().getPort());
            stopDeferred = Promise.defer();
            instance = this;
            getVertx().deployVerticle(VertxHttpServerVerticle.class.getName(), verticleDeploymentOptions, event -> {
                if (event.succeeded()) {
                    verticleDeploymentId = event.result();
                    startDeferred.resolve(null);
                    log.info("Started HTTP Server on port " + serverOptions.getPort());
                } else {
                    try {
                        startDeferred.reject(event.cause());
                    } catch(IllegalStateException ignore) { }
                }
            });
        } else {
            startDeferred.reject(new IllegalStateException("Already running"));
        }

        return startDeferred.getPromise();
    }

    public void awaitStop() throws Exception {
        Promise<Void> stopDeferred = this.stopDeferred.getPromise();
        if (stopDeferred != null) {
            try {
                stopDeferred.get();
            } catch (ExecutionException e) {
                throwCasted(e);
            }
        }
    }

    public Promise<Void> stop() {
        if (!stopped) {
            synchronized (this) {
                if (stopped) {
                    return stopDeferred.getPromise();
                }
                stopped = true;
            }

            if (verticleDeploymentId != null) {
                log.info("Stopping HTTP Server...");
                getVertx().undeploy(verticleDeploymentId, event1 -> {
                    getVertx().close(event2 -> {
                        VertxUtil.bindAsyncResultToDeferred(event2, stopDeferred);
                        verticleDeploymentId = null;
                        stopDeferred = null;
                        log.info("Stopped HTTP Server");
                    });
                });
            } else {
                stopDeferred.resolve();
            }
        }
        return stopDeferred.getPromise();
    }

    @Override
    public void close() throws Exception {
        stop();
        awaitStop();
    }

    private static void throwCasted(Throwable throwable) throws Exception {
        if (throwable instanceof RuntimeException) {
            throw (RuntimeException) throwable;
        }
        if (throwable instanceof Exception) {
            throw (Exception) throwable;
        }
        if (throwable instanceof Error) {
            throw (Error) throwable;
        }
        throw new RuntimeException(throwable);
    }
}
