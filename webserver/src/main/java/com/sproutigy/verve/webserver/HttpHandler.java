package com.sproutigy.verve.webserver;

public interface HttpHandler {
    Object handle(HttpRequestContext ctx) throws Exception;
}
