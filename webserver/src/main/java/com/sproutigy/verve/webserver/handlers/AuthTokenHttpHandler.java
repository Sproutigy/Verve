package com.sproutigy.verve.webserver.handlers;

import com.google.inject.Inject;
import com.sproutigy.verve.webserver.HttpHandler;
import com.sproutigy.verve.webserver.HttpRequestContext;
import com.sproutigy.verve.webserver.HttpUtil;
import com.sproutigy.verve.webserver.annotations.HttpRoute;
import com.sproutigy.verve.webserver.auth.AuthUtil;
import com.sproutigy.verve.webserver.auth.TokenService;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.cookie.Cookie;

@HttpRoute(order = -Integer.MAX_VALUE)
public class AuthTokenHttpHandler implements HttpHandler {

    private TokenService tokenService;

    @Inject
    public AuthTokenHttpHandler(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    public static final String COOKIE_NAME = "AUTH-TOKEN";

    public static final String ATTRIBUTE = "auth-token";


    @Override
    public Object handle(HttpRequestContext ctx) throws Exception {
        String token = getClientTokenString(ctx);
        if (token != null) {
            if (tokenService.verifyToken(token)) {
                ctx.getAttributes().set(ATTRIBUTE, token);
                String id = tokenService.getIdFromToken(token);
                if (id != null) {
                    AuthUtil.setId(ctx, id);
                }
            }
        }
        return ctx.proceed();
    }

    public static String getVerifiedTokenString(HttpRequestContext ctx) {
        return ctx.getAttributes().get(ATTRIBUTE);
    }


    private static String getClientTokenString(HttpRequestContext ctx) {
        String tokenString = ctx.getRequest().getHeader(HttpHeaderNames.AUTHORIZATION);
        if (tokenString != null) {
            if (tokenString.startsWith("Bearer ")) {
                tokenString = tokenString.substring(7);
            } else {
                tokenString = null;
            }
        }

        if (tokenString == null) {
            tokenString = ctx.getRequest().getCookieValue(COOKIE_NAME);
        }

        return tokenString;
    }


    public static void setTokenCookie(HttpRequestContext ctx, String token, String path, boolean secure, boolean remember) {
        Cookie cookie = HttpUtil.cookie(COOKIE_NAME, token);
        cookie.setPath(path);
        cookie.setHttpOnly(true);
        cookie.setSecure(secure);
        if (remember) {
            cookie.setMaxAge(60 * 24 * 365);
        }
        ctx.getResponse().setCookie(cookie);
    }

    public static void removeTokenCookie(HttpRequestContext ctx, String path) {
        Cookie cookie = ctx.getRequest().getCookie(COOKIE_NAME);
        if (cookie != null) {
            cookie.setPath(path);
            ctx.getResponse().removeCookie(cookie);
        }
    }

}
