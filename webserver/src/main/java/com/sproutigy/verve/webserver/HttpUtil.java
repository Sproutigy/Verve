package com.sproutigy.verve.webserver;

import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.vertx.core.http.HttpHeaders;

import javax.annotation.Nullable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

public final class HttpUtil {
    private HttpUtil() { }

    public static void cacheForever(HttpResponse response) {
        cacheForever(response, false);
    }

    public static void cacheForever(HttpResponse response, boolean isPublic) {
        if (isPublic) {
            response.setHeader(HttpHeaders.CACHE_CONTROL, "max-age=31556926, max-stale=31556926, public");
        } else {
            response.setHeader(HttpHeaders.CACHE_CONTROL, "max-age=31556926, max-stale=31556926");
        }
        response.setHeader(HttpHeaders.EXPIRES, "Sun, 17-Jan-2038 19:14:07 GMT");
    }

    public static void cacheAvoid(HttpResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-store");
    }

    public static boolean isLocalHost(HttpRequest req) {
        String host = req.getHeader("Host");
        if (host != null && host.startsWith("localhost")) {
            return true;
        }
        return false;
    }

    @Nullable
    public static String urlencode(String s) {
        if (s == null) {
            return null;
        }
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Nullable
    public static String urldecode(String s) {
        if (s == null) {
            return null;
        }
        try {
            return URLDecoder.decode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static Cookie cookie(String name, @Nullable String value) {
        if (value == null) {
            value = "";
        }
        return new DefaultCookie(name, value);
    }
}
