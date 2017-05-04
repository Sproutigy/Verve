package com.sproutigy.verve.webserver;

import com.sproutigy.commons.async.Promise;
import com.sproutigy.commons.binary.Binary;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.cookie.Cookie;

public interface HttpResponse {
    HttpResponse status(int code);
    HttpResponse status(HttpResponseStatus status);

    HttpResponse addHeader(CharSequence name, CharSequence value);
    HttpResponse setHeader(CharSequence name, CharSequence value);
    HttpResponse setHeaderIfNotSet(CharSequence name, CharSequence value);

    HttpResponse setCookie(Cookie cookie);
    HttpResponse setCookie(String cookie);
    HttpResponse removeCookie(Cookie cookie);
    HttpResponse removeCookie(String cookieName, String path);

    Promise<Void> sendFile(String filePath);

    void redirect(String url);

    void end(String text);
    void end(Binary data);
    void end();

    boolean isHeaderCompleted();
    boolean isFinalized();
}
