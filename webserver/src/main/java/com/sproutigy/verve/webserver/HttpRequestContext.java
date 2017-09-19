package com.sproutigy.verve.webserver;

import com.sproutigy.commons.async.Promise;
import com.sproutigy.commons.binary.Binary;
import com.sproutigy.verve.module.Context;
import io.vertx.core.Vertx;

import java.util.Map;
import java.util.concurrent.Callable;

public interface HttpRequestContext {
    Object FINISH = new Object();
    Object PROCEED = new Object();
    Object HANDLED = new Object();

    default Object finish() {
        return FINISH;
    }

    default Object proceed() {
        return PROCEED;
    }

    default Object handled() {
        return HANDLED;
    }

    String getContextPath();

    void thrown(Throwable throwable);

    Promise<Binary> fetchData();
    Promise<Binary> fetchData(long limit);
    <T> Promise<T> fetchDataAs(Class<T> clazz);
    <T> Promise<T> fetchDataAs(Class<T> clazz, long limit);

    void sendObject(Object o);

    HttpRequest getRequest();
    HttpResponse getResponse();

    HttpRoute getRoute();
    Map<String, String> getRouteParams();

    HttpHandler getHandler();

    HttpServer getServer();

    Vertx getVertx();

    void shutdownServer();

    void onFinished(Runnable runnable);

    <V> Promise<V> blocking(Callable<V> callable);

    Context getAttributes();


    ThreadLocal<HttpRequestContext> threadLocalHttpContext = new ThreadLocal<>();


    static HttpRequestContext get() {
        HttpRequestContext context = threadLocalHttpContext.get();
        if (context == null) {
            throw new IllegalStateException("HttpRequestContext is not available in current context");
        }
        return context;
    }

    static void set(HttpRequestContext ctx) {
        threadLocalHttpContext.set(ctx);
    }

    static void remove() {
        threadLocalHttpContext.remove();
    }

}
