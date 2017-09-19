package com.sproutigy.verve.webserver.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sproutigy.commons.async.Deferred;
import com.sproutigy.commons.async.FutureWatch;
import com.sproutigy.commons.async.Promise;
import com.sproutigy.commons.binary.Binary;
import com.sproutigy.verve.module.Context;
import com.sproutigy.verve.module.ContextImpl;
import com.sproutigy.verve.webserver.*;
import com.sproutigy.verve.webserver.actions.FinishWebAction;
import com.sproutigy.verve.webserver.actions.WebAction;
import com.sproutigy.verve.webserver.exceptions.HttpException;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Vertx;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;

@Slf4j
@RequiredArgsConstructor
public class HttpRequestContextImpl implements Runnable, HttpRequestContext {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Getter
    @NonNull
    private HttpServer server;

    @Getter
    @NonNull
    private String contextPath;

    @Getter
    @NonNull
    private HttpRequest request;

    @Getter
    @NonNull
    private HttpResponse response;

    @Getter
    @NonNull
    private Iterator<HttpRoute> routesWithHandlersIterator;

    private HttpRoute currentRoute = null;
    private Map<String, String> routeParams = null;

    private CopyOnWriteArrayList<Runnable> onFinishedCallbacks = new CopyOnWriteArrayList<>();

    @Getter
    @NonNull
    private Context attributes = new ContextImpl();

    private volatile boolean finished;


    public synchronized void run() {
        HttpRequestContext.set(this);
        try {
            while (true) {
                if (routesWithHandlersIterator.hasNext()) {
                    currentRoute = routesWithHandlersIterator.next();
                    routeParams = getRoute().getFilter().filter(getRequest());
                    if (routeParams != null) {
                        try {
                            Object ret = getHandler().handle(this);
                            handleReturnedObject(ret);
                        } catch (Throwable throwable) {
                            thrown(throwable);
                        }
                        return;
                    }
                } else {
                    if (!finished) {
                        finished = true;
                        for (Runnable runnable : onFinishedCallbacks) {
                            try {
                                runnable.run();
                            } catch (Throwable e) {
                                log.warn("On request context {} finished listener {} thrown exception", this, runnable, e);
                            }
                        }
                    }
                    if (!getResponse().isFinalized()) {
                        getResponse().status(HttpResponseStatus.NOT_FOUND).end();
                    }
                    return;
                }
            }
        } finally {
            HttpRequestContext.remove();
        }
    }

    @Override
    public void thrown(Throwable throwable) {
        if (throwable instanceof HttpException) {
            HttpException httpException = ((HttpException) throwable);
            String trace = throwable.getStackTrace()[0].toString();
            log.debug("HTTP {} thrown within request {} at {}", httpException, request, trace);
            getResponse().status(((HttpException) throwable).getStatusCode()).end();
        } else {
            log.error("Unhandled exception thrown within request {}", request, throwable);
            try {
                getResponse().status(HttpResponseStatus.INTERNAL_SERVER_ERROR).end();
            } catch (Exception ignore) {
            }
        }
    }

    @Override
    public Promise<Binary> fetchData() {
        return fetchData(0);
    }

    @Override
    public Promise<Binary> fetchData(long limit) {
        return getRequest().fetchData(limit);
    }

    @Override
    public <T> Promise<T> fetchDataAs(Class<T> clazz) {
        return fetchDataAs(clazz, 0);
    }

    @Override
    public <T> Promise<T> fetchDataAs(Class<T> clazz, long limit) {
        Deferred<T> deferred = Promise.defer();
        FutureWatch.listen(getRequest().fetchData(limit), (result, value, cause) -> {
            if (result.isSuccess()) {
                try {
                    T obj = deserialize(value, clazz);
                    deferred.resolve(obj);
                } catch (Exception e) {
                    deferred.reject(new HttpException(HttpResponseStatus.BAD_REQUEST, e));
                }
            } else {
                deferred.reject(cause);
            }
        });
        return deferred.getPromise();
    }

    @SuppressWarnings("unchecked")
    private void handleReturnedObject(Object ret) {
        if (ret == HANDLED) return;

        HttpRequestContext.set(this);

        try {
            if (ret != null && ret != PROCEED) {
                if (ret == FINISH || ret instanceof FinishWebAction) {
                    try {
                        getResponse().end();
                    } catch (IllegalStateException ignore) {
                    }
                    return;
                }

                if (getResponse().isFinalized()) {
                    log.warn("Response of {} is already finalized, cannot respond", this);
                } else {
                    if (ret instanceof WebAction) {
                        ((WebAction)ret).execute(this);
                        return;
                    }

                    if (ret instanceof Deferred) {
                        handleReturnedObject (((Deferred) ret).getPromise());
                        return;
                    }

                    if (ret instanceof Future) {
                        FutureWatch.listen((Future<?>) ret, (result, value, cause) -> {
                            getVertx().runOnContext(aVoid -> {
                                if (result.isSuccess()) {
                                    handleReturnedObject(value);
                                } else {
                                    thrown(cause);
                                }
                            });
                        });
                        return;
                    }

                    if (ret instanceof Callable) {
                        handleReturnedObject(blocking((Callable) ret));
                        return;
                    }

                    if (ret instanceof Runnable) {
                        handleReturnedObject(blocking((Callable<Void>) () -> {
                            ((Runnable) ret).run();
                            return null;
                        }));
                        return;
                    }

                    sendObject(ret);
                }
            }

            run(); //next

        } catch (Throwable throwable) {
            thrown(throwable);
        } finally {
            HttpRequestContext.remove();
        }
    }

    @Override
    public void sendObject(Object o) {
        Binary data;
        if (o == null) {
            data = Binary.EMPTY;
        } else if (isPrimitive(o)) {
            getResponse().setHeaderIfNotSet(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
            data = Binary.fromString(o.toString(), StandardCharsets.UTF_8);
        } else if (o instanceof byte[]) {
            data = Binary.from((byte[]) o);
        } else if (o instanceof Binary) {
            data = (Binary) o;
        } else {
            try {
                data = serialize(o);
            } catch (Exception e) {
                log.warn("Could not serialize object {}", o, e);
                getResponse().status(HttpResponseStatus.INTERNAL_SERVER_ERROR).end();
                return;
            }
            getResponse().setHeaderIfNotSet(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
        }

        getResponse().setHeaderIfNotSet(HttpHeaderNames.CONTENT_TYPE, "application/octet-stream");
        getResponse().setHeaderIfNotSet(HttpHeaderNames.CONTENT_LENGTH, Long.toString(data.length()));
        if (!Objects.equals(getRequest().getMethod(), HttpMethod.HEAD.name())) {
            getResponse().end(data);
        } else {
            getResponse().end();
        }
    }

    @Override
    public HttpRoute getRoute() {
        return currentRoute;
    }

    @Override
    public Map<String, String> getRouteParams() {
        return routeParams;
    }

    @Override
    public HttpHandler getHandler() {
        return currentRoute.getHandler();
    }

    @Override
    public Vertx getVertx() {
        return server.getVertxHttpServer().getVertx();
    }

    @Override
    public void shutdownServer() {
        server.stop();
    }

    @Override
    public void onFinished(Runnable runnable) {
        onFinishedCallbacks.add(runnable);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V> Promise<V> blocking(Callable<V> callable) {
        Deferred<V> deferred = Promise.defer();
        getVertx().executeBlocking(future -> {
            try {
                future.complete(callable.call());
            } catch (Throwable e) {
                future.fail(e);
            }
        }, ret -> {
            if (ret.succeeded()) {
                deferred.resolve((V) ret.result());
            } else {
                deferred.reject(ret.cause());
            }
        });
        return deferred.getPromise();
    }

    protected Binary serialize(Object o) throws Exception {
        return Binary.from(mapper.writeValueAsBytes(o));
    }

    protected <T> T deserialize(Binary binary, Class<T> clazz) throws Exception {
        byte[] data = binary.asByteArray(false);
        if (data.length > 0) {
            return mapper.readValue(data, clazz);
        } else {
            return null;
        }
    }

    private static boolean isPrimitive(Object o) {
        return o instanceof String || o instanceof Long || o instanceof Integer || o instanceof Short || o instanceof Byte || o instanceof Double || o instanceof Float || o instanceof BigInteger || o instanceof BigDecimal;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append(getRequest().getMethod()).append(" ").append(getRequest().getPath());
        if (getRequest().getQueryString() != null && !getRequest().getQueryString().isEmpty()) {
            builder.append("?").append(getRequest().getQueryString());
        }

        return builder.toString();
    }
}
