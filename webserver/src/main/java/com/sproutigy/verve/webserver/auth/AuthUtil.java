package com.sproutigy.verve.webserver.auth;

import com.sproutigy.verve.webserver.HttpRequestContext;

public class AuthUtil {

    private AuthUtil() { }

    private static final String ATTRIBUTE = "auth";


    public static boolean isAuthenticated(HttpRequestContext ctx) {
        String id = getId(ctx);
        if (id == null || id.isEmpty()) {
            return false;
        }
        return true;
    }

    public static String getId(HttpRequestContext ctx) {
        return ctx.getAttributes().get(ATTRIBUTE);
    }

    public static void setId(HttpRequestContext ctx, String id) {
        ctx.getAttributes().set(ATTRIBUTE, id);
    }
}
