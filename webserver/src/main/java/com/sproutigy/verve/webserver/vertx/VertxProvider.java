package com.sproutigy.verve.webserver.vertx;

import io.vertx.core.Vertx;
import io.vertx.core.impl.FileResolver;

class VertxProvider {
    private VertxProvider() {
    }

    static {
        System.setProperty(FileResolver.DISABLE_FILE_CACHING_PROP_NAME, "true");
        System.setProperty(FileResolver.DISABLE_CP_RESOLVING_PROP_NAME, "true");
    }

    public static Vertx provide() {
        return Vertx.vertx();
    }

}
