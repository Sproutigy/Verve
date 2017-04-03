package com.sproutigy.verve.webserver.handlers;

import com.sproutigy.commons.binary.Binary;
import com.sproutigy.verve.webserver.HttpRequestContext;
import com.sproutigy.verve.webserver.HttpHandler;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.http.HttpHeaders;

import java.util.function.Function;

public class RedirectHttpHandler implements HttpHandler {

    private Function<HttpRequestContext, String> redirectFunction;

    private HttpResponseStatus responseStatus;

    public RedirectHttpHandler(Function<HttpRequestContext, String> redirectFunction) {
        this(redirectFunction, false);
    }

    public RedirectHttpHandler(Function<HttpRequestContext, String> redirectFunction, boolean permanently) {
        this.redirectFunction = redirectFunction;
        if (permanently) {
            this.responseStatus = HttpResponseStatus.MOVED_PERMANENTLY;
        } else {
            this.responseStatus = HttpResponseStatus.TEMPORARY_REDIRECT;
        }
    }

    @Override
    public Object handle(HttpRequestContext ctx) throws Exception {
        String url = redirectFunction.apply(ctx);
        if (url != null && !url.isEmpty()) {
            ctx.getResponse()
                    .status(responseStatus)
                    .setHeader(HttpHeaders.LOCATION, url)
                    .setHeader(HttpHeaders.CONTENT_TYPE, "text/html; charset=UTF-8")
                    .end(Binary.fromString("<html><head></head><body><a href=\""+url+"\">"+url+"</a></body></html>"));
        }
        return null;
    }
}
