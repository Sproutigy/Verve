package com.sproutigy.verve.webserver.impl;

import com.sproutigy.commons.binary.Binary;
import com.sproutigy.verve.webserver.HttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.vertx.core.http.HttpHeaders;

import java.nio.charset.StandardCharsets;

public abstract class AbstractHttpResponse implements HttpResponse {

    @Override
    public HttpResponse status(HttpResponseStatus status) {
        return status(status.code());
    }

    @Override
    public HttpResponse setCookie(Cookie cookie) {
        return setCookie(ServerCookieEncoder.STRICT.encode(cookie));
    }

    @Override
    public HttpResponse setCookie(String cookie) {
        return setHeader(HttpHeaders.SET_COOKIE, cookie);
    }

    @Override
    public void redirect(String url) {
        status(HttpResponseStatus.TEMPORARY_REDIRECT);
        setHeader("Location", url);
        end();
    }

    @Override
    public void end(String text) {
        if (text != null) {
            Binary data = Binary.fromString(text, StandardCharsets.UTF_8);
            setHeaderIfNotSet(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
            setHeaderIfNotSet(HttpHeaderNames.CONTENT_LENGTH, Long.toString(data.length()));
            end(data);
        } else {
            end();
        }
    }
}
