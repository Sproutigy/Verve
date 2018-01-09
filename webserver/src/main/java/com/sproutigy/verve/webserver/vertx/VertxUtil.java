package com.sproutigy.verve.webserver.vertx;

import com.sproutigy.commons.async.Deferred;
import io.vertx.core.AsyncResult;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;

@Slf4j
public final class VertxUtil {
    private VertxUtil() {
    }

    public static void fail(HttpServerRequest request, Throwable cause) {
        fail(request.response(), cause);
    }

    public static void fail(HttpServerResponse response, Throwable cause) {
        log.error("Request failure", cause);
        response.setStatusCode(500);
        response.end();
    }

    public static void endQuietly(HttpServerRequest request) {
        endQuietly(request.response());
        try {
            request.resume();
        } catch (IllegalStateException ignore) {
        }
    }

    public static void endQuietly(HttpServerRequest request, Integer statusCode) {
        endQuietly(request.response(), statusCode);
        try {
            request.resume();
        } catch (IllegalStateException ignore) {
        }
    }

    public static void endQuietly(HttpServerRequest request, Integer statusCode, String message) {
        endQuietly(request.response(), statusCode, message);
        try {
            request.resume();
        } catch (IllegalStateException ignore) {
        }
    }

    public static void endQuietly(HttpServerResponse response) {
        endQuietly(response, null, null);
    }

    public static void endQuietly(HttpServerResponse response, Integer statusCode) {
        endQuietly(response, statusCode, null);
    }

    public static void endQuietly(HttpServerResponse response, Integer statusCode, String message) {
        if (statusCode != null) {
            try {
                response.setStatusCode(statusCode);
            } catch (IllegalStateException ignore) {
            }
        }

        try {
            if (message != null) {
                response.end(message);
            } else {
                response.end();
            }
        } catch (IllegalStateException ignore) {
        }
    }

    public static String describeRequest(HttpServerRequest request) {
        StringBuilder builder = new StringBuilder();
        if (request.method() != null) {
            builder.append(request.method().name()).append(' ');
        }
        if (request.path() != null) {
            builder.append(request.path());
        }
        if (request.query() != null) {
            builder.append("?");
            builder.append(request.query());
        }
        if (request.remoteAddress() != null && request.remoteAddress().host() != null) {
            builder.append(" [").append(request.remoteAddress().host()).append("]");
        }
        return builder.toString();
    }

    public static <T> T runCatchFail(HttpServerRequest request, Callable<T> callable) {
        try {
            return callable.call();
        } catch (Throwable e) {
            fail(request, e);
            return null;
        }
    }

    public static void runCatchFail(HttpServerRequest request, Runnable runnable) {
        try {
            runnable.run();
        } catch (Throwable e) {
            fail(request, e);
        }
    }

    public static <V> void bindAsyncResultToDeferred(AsyncResult<V> asyncResult, Deferred<V> deferred) {
        if (asyncResult.succeeded()) {
            deferred.resolve(asyncResult.result());
        } else {
            deferred.reject(asyncResult.cause());
        }
    }
}
