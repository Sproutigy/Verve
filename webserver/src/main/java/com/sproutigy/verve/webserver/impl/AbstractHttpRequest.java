package com.sproutigy.verve.webserver.impl;

import com.sproutigy.commons.async.Promise;
import com.sproutigy.commons.binary.Binary;
import com.sproutigy.verve.webserver.HttpRequest;
import com.sproutigy.verve.webserver.exceptions.BadRequestHttpException;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;

import javax.annotation.Nullable;
import java.util.*;

public abstract class AbstractHttpRequest implements HttpRequest {
    private Set<Cookie> cookies;
    private Map<String, String> params;

    @Override
    public String getHeaderRequired(CharSequence name) throws BadRequestHttpException {
        String value = getHeader(name);
        if (value == null) {
            throw new BadRequestHttpException();
        }
        return value;
    }

    @Override
    public Promise<Binary> fetchData() {
        return fetchData(0);
    }

    @Override
    public Map<String, String> getQueryParams() {
        if (this.params == null) {
            Map<String, String> params = new LinkedHashMap<>();
            for (String name : getQueryParamNames()) {
                params.put(name, getQueryParam(name));
            }
            this.params = Collections.unmodifiableMap(params);
        }
        return this.params;
    }

    @Override
    public String getQueryParamRequired(String name) throws BadRequestHttpException {
        String value = getQueryParam(name);
        if (value == null) {
            throw new BadRequestHttpException();
        }
        return value;
    }

    @Override
    public Set<Cookie> getCookies() {
        if (cookies == null) {
            String cookieHeader = getHeader(HttpHeaderNames.COOKIE.toString());
            if (cookieHeader != null) {
                cookies = Collections.unmodifiableSet(ServerCookieDecoder.LAX.decode(cookieHeader));
            } else {
                cookies = Collections.emptySet();
            }
        }
        return cookies;
    }

    @Nullable
    @Override
    public Cookie getCookie(CharSequence name) {
        for (Cookie cookie : getCookies()) {
            if (Objects.equals(cookie.name(), name)) {
                return cookie;
            }
        }
        return null;
    }

    @Nullable
    @Override
    public String getCookieValue(CharSequence name) {
        Cookie cookie = getCookie(name);
        if (cookie != null) {
            return cookie.value();
        }
        return null;
    }

    @Override
    public String getRemote() {
        return null;
    }
}
