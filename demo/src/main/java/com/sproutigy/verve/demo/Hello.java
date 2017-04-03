package com.sproutigy.verve.demo;

import com.google.inject.Singleton;
import com.sproutigy.verve.webserver.HttpObjectHandler;
import com.sproutigy.verve.webserver.HttpRequestContext;
import com.sproutigy.verve.webserver.annotations.HttpRoute;

/**
 * Handler that returns "Hello World" string
 *
 * HttpObjectHandler may input and output any kind of class.
 * It automatically serializes and deserializes input/output objects.
 */
@Singleton
@HttpRoute(path = "/hello")
public class Hello implements HttpObjectHandler<String, String> {
    @Override
    public String handle(String input, HttpRequestContext ctx) throws Exception {
        return "Hello World";
    }
}
