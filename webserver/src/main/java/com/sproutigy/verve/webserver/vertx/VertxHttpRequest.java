package com.sproutigy.verve.webserver.vertx;

import com.sproutigy.commons.async.Deferred;
import com.sproutigy.commons.async.Promise;
import com.sproutigy.commons.binary.Binary;
import com.sproutigy.commons.binary.BinaryBuilder;
import com.sproutigy.verve.webserver.HttpRequest;
import com.sproutigy.verve.webserver.impl.AbstractHttpRequest;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

import javax.annotation.Nullable;
import java.util.Collection;

public class VertxHttpRequest extends AbstractHttpRequest implements HttpRequest {
    private final HttpServerRequest vertxRequest;

    private volatile Deferred<Binary> dataDeferred = null;

    public VertxHttpRequest(HttpServerRequest vertxRequest) {
        this.vertxRequest = vertxRequest;
    }

    public HttpServerRequest asVertxRequest() {
        return vertxRequest;
    }

    public HttpServerResponse asVertxResponse() {
        return vertxRequest.response();
    }

    @Override
    public String getPath() {
        return vertxRequest.path();
    }

    @Nullable
    @Override
    public String getHeader(CharSequence name) {
        return asVertxRequest().getHeader(name);
    }

    @Override
    public String getMethod() {
        return asVertxRequest().method().toString();
    }

    public String getContextualPath() {
        return vertxRequest.path();
    }

    @Override
    public Promise<Binary> fetchData() {
        synchronized (this) {
            if (dataDeferred != null) {
                return dataDeferred.getPromise();
            }
            dataDeferred = Promise.defer();
        }

        BinaryBuilder binaryBuilder = new BinaryBuilder();

        try {
            HttpServerRequest req = asVertxRequest();
            req.pause();
            req.handler(buffer -> binaryBuilder.append(buffer.getBytes()));
            req.endHandler(aVoid -> dataDeferred.resolve(binaryBuilder.build()));
            req.exceptionHandler(cause -> dataDeferred.reject(cause));
            req.resume();
        } catch (IllegalStateException e) {
            dataDeferred.reject(e);
        }

        return dataDeferred.getPromise();
    }

    @Override
    public String getQueryString() {
        return vertxRequest.query();
    }

    @Nullable
    @Override
    public String getQueryParam(String name) {
        return vertxRequest.getParam(name);
    }

    @Override
    public Collection<String> getQueryParamNames() {
        return vertxRequest.params().names();
    }

    @Override
    public String getRemote() {
        return asVertxRequest().remoteAddress().host();
    }

    @Override
    public void close() throws Exception {
        vertxRequest.response().end();
    }

    @Override
    public String toString() {
        return VertxUtil.describeRequest(asVertxRequest());
    }
}
