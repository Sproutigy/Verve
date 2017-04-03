package com.sproutigy.verve.demo;

import com.google.inject.Singleton;
import com.sproutigy.verve.webserver.annotations.HttpRoute;
import com.sproutigy.verve.webserver.HttpRequestContext;
import com.sproutigy.verve.webserver.HttpHandler;

import java.util.Random;
import java.util.concurrent.Callable;

/**
 * Randomizes integer number
 *
 * When HttpHandler returns Callable or Runnable,
 * it means that task may take long time and it should be run in worker thread
 */
@Singleton
@HttpRoute(path = "/rnd")
@HttpRoute(path = "/random")
public class Randomizer implements HttpHandler {
    @Override
    public Object handle(HttpRequestContext ctx) throws Exception {
        return (Callable) () -> {
            Random rnd = new Random();
            return rnd.nextInt();
        };
    }
}
