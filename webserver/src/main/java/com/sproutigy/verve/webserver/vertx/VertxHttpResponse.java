package com.sproutigy.verve.webserver.vertx;

import com.sproutigy.commons.async.Deferred;
import com.sproutigy.commons.async.Promise;
import com.sproutigy.commons.binary.Binary;
import com.sproutigy.verve.webserver.HttpResponse;
import com.sproutigy.verve.webserver.impl.AbstractHttpResponse;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VertxHttpResponse extends AbstractHttpResponse {
    private HttpServerRequest vertxRequest;
    private HttpServerResponse vertxResponse;
    private volatile boolean finalized = false;

    public VertxHttpResponse(HttpServerRequest vertxRequest) {
        this.vertxRequest = vertxRequest;
        this.vertxResponse = vertxRequest.response();
    }

    public HttpServerResponse asVertxResponse() {
        return vertxResponse;
    }

    @Override
    public HttpResponse status(int statusCode) {
        asVertxResponse().setStatusCode(statusCode);
        return this;
    }

    @Override
    public HttpResponse setHeader(CharSequence name, CharSequence value) {
        asVertxResponse().headers().add(name, value);
        return this;
    }

    @Override
    public HttpResponse setHeaderIfNotSet(CharSequence name, CharSequence value) {
        if (!asVertxResponse().headers().contains(name)) {
            setHeader(name, value);
        }
        return this;
    }

    @Override
    public Promise<Void> sendFile(String filePath) {
        Deferred<Void> deferred = Promise.defer();
        finalized = true;
        asVertxResponse().sendFile(filePath, new Handler<AsyncResult<Void>>() {
            @Override
            public void handle(AsyncResult<Void> result) {
                if (result.succeeded()) {
                    deferred.resolve();
                } else {
                    deferred.reject(result.cause());
                }
            }
        });
        return deferred.getPromise();
    }

    @Override
    public void end(Binary data) {
        try {
            if (data != null) {
                if (data.hasLength()) {
                    setHeaderIfNotSet(HttpHeaders.CONTENT_LENGTH, Long.toString(data.length()));
                }
                finalized = true;
                asVertxResponse().end(Buffer.buffer(data.asByteArray()));
            } else {
                end();
            }
        } catch (Throwable e) {
            log.warn("Could not gracefully end response", e);
        }
    }

    @Override
    public void end() {
        try {
            finalized = true;
            asVertxResponse().end();
        } catch (Throwable e) {
            log.warn("Could not gracefully end response", e);
        }

        try {
            vertxRequest.resume();
        } catch (IllegalStateException ignore) {
        }
    }

    @Override
    public boolean isHeaderCompleted() {
        return asVertxResponse().headWritten();
    }

    public boolean isFinalized() {
        if (!finalized) {
            return false;
        }

        try {
            asVertxResponse().setChunked(asVertxResponse().isChunked());
            return false;
        } catch (IllegalStateException e) {
            return true;
        }
    }
}
