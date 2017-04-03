package com.sproutigy.verve.webserver.vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class VertxHttpServerVerticle extends AbstractVerticle {

    private HttpServerOptions options;
    private Handler<HttpServerRequest> requestHandler;
    private HttpServer httpServer;

    public VertxHttpServerVerticle() {
        this(VertxHttpServer.instance);
    }

    public VertxHttpServerVerticle(VertxHttpServer vertxHttpServer) {
        this(vertxHttpServer.getServerOptions(), vertxHttpServer);
    }

    public VertxHttpServerVerticle(HttpServerOptions options, Handler<HttpServerRequest> requestHandler) {
        this.options = options;
        this.requestHandler = requestHandler;
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        httpServer = getVertx().createHttpServer(options)
                .requestHandler(requestHandler)
                .listen(options.getPort(), result -> {
                    if (result.succeeded()) {
                        startFuture.complete();
                    } else {
                        startFuture.fail(result.cause());
                    }
                });
    }

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {
        httpServer.close(voidAsyncResult -> stopFuture.complete());
    }
}
