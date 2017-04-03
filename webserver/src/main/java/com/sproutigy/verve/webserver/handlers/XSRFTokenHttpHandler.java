package com.sproutigy.verve.webserver.handlers;

import com.sproutigy.verve.webserver.HttpHandler;
import com.sproutigy.verve.webserver.HttpRequestContext;
import com.sproutigy.verve.webserver.annotations.HttpRoute;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.vertx.core.http.HttpHeaders;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@HttpRoute(order = -Integer.MAX_VALUE)
public class XSRFTokenHttpHandler implements HttpHandler {
    private final static Collection<String> SAFE_HEADERS = Arrays.asList("HEAD", "GET", "OPTIONS", "TRACE");

    public static final String[] HTTP_HEADERS = new String[] { "X-XSRF-TOKEN", "X-CSRF-TOKEN" };
    public static final String COOKIE_NAME = "XSRF-TOKEN";
    public static final String FETCH_ACTION = "Fetch";

    @Override
    public Object handle(HttpRequestContext ctx) throws Exception {
        String header = null;
        String clientToken = null;

        for (String possibleHeader : HTTP_HEADERS) {
            String value = ctx.getRequest().getHeader(possibleHeader);
            if (value != null) {
                header = possibleHeader;
                clientToken = value;
            }
        }

        String validToken = null;

        if (Objects.equals(clientToken, FETCH_ACTION)) {
            validToken = getOrCreateToken(ctx);
            ctx.getResponse().setHeader(header, validToken);
        }

        if (shouldCheck(ctx)) {
            if (validToken == null) {
                validToken = getOrCreateToken(ctx);
            }

            if (!Objects.equals(validToken, clientToken)) {
                log.warn("CSRF/XSRF token validation failed, client:'{}' vs valid:'{}' in {}", clientToken, validToken, ctx);
                ctx.getResponse().status(HttpResponseStatus.FORBIDDEN).end("CSRF/XSRF token validation failed");
                return ctx.finish();
            }
        } else {
            String token = getOrCreateToken(ctx);
            if (getTokenCookie(ctx) == null) {
                Cookie cookie = createCookieFromToken(token);
                ctx.getResponse().setCookie(cookie);
            }
        }

        return ctx.proceed();
    }

    protected Cookie getTokenCookie(HttpRequestContext ctx) {
        return ctx.getRequest().getCookie(COOKIE_NAME);
    }

    protected boolean shouldCheck(HttpRequestContext ctx) {
        String referer = ctx.getRequest().getHeader(HttpHeaders.REFERER);
        return referer != null && !SAFE_HEADERS.contains(ctx.getRequest().getMethod());
    }

    protected String getOrCreateToken(HttpRequestContext ctx) {
        Cookie cookie = getTokenCookie(ctx);
        if (cookie != null) {
            return cookie.value();
        }
        return UUID.randomUUID().toString();
    }

    protected Cookie createCookieFromToken(String token) {
        DefaultCookie cookie = new DefaultCookie(COOKIE_NAME, token);
        cookie.setHttpOnly(false);
        cookie.setPath("/");
        return cookie;
    }
}
