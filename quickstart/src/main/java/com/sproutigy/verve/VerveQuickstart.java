package com.sproutigy.verve;

import com.sproutigy.verve.module.VerveModule;
import com.sproutigy.verve.module.VerveRoot;
import com.sproutigy.verve.module.VerveRootListener;
import com.sproutigy.verve.quickstart.LocalCORS;
import com.sproutigy.verve.webserver.HttpRoute;
import com.sproutigy.verve.webserver.HttpRouter;
import com.sproutigy.verve.webserver.HttpServer;
import com.sproutigy.verve.webserver.impl.HttpRouterImpl;
import lombok.extern.slf4j.Slf4j;

import java.net.BindException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;

@Slf4j
public class VerveQuickstart {
    private VerveQuickstart() { }

    public static final int FAILURE_EXIT_CODE = 500;
    public static final int BIND_ADDRESS_IN_USE_EXIT_CODE = 900;
    public static final int BOOT_MODULES_FAIL_EXIT_CODE = 999;

    private static final String LOGO = "\n" +
            " _    __                   \n" +
            "| |  / /__  ______   _____ \n" +
            "| | / / _ \\/ ___/ | / / _ \\\n" +
            "| |/ /  __/ /   | |/ /  __/\n" +
            "|___/\\___/_/    |___/\\___/ \n" +
            "                           \n";


    public static void main(String[] args) throws Exception {
        System.out.println(LOGO);

        HttpServer httpServer = new HttpServer();
        try {
            httpServer.start().get();
        } catch (Exception e) {
            Throwable failure = e;
            if (failure instanceof ExecutionException) {
                failure = e.getCause();
            }

            log.error("HTTP Server error", failure);

            if (failure instanceof BindException) {
                System.exit(BIND_ADDRESS_IN_USE_EXIT_CODE);
            } else {
                System.exit(FAILURE_EXIT_CODE);
            }
        }

        VerveRoot verveRoot = new VerveRoot();
        verveRoot.addListener((eventType, module) -> {
            if (eventType == VerveRootListener.EventType.Initializing) {
                module.getContext().add(HttpServer.class, httpServer);
                module.getContext().add(HttpRouter.class, new HttpRouterImpl());
            }
        });

        try {
            verveRoot.autoDetectModules();
        } catch (Throwable e) {
            log.error("Verve modules boot error", e);
            System.exit(BOOT_MODULES_FAIL_EXIT_CODE);
        }

        LocalCORS cors = new LocalCORS();
        HttpRouter rootRouter = new HttpRouterImpl();
        rootRouter.add(cors);

        httpServer.setRouteProvider(() -> {
            Collection<HttpRoute> routes = new LinkedList<>();
            for (HttpRoute route : rootRouter) {
                routes.add(route);
            }

            for (VerveModule module : verveRoot) {
                HttpRouter router = module.getContext().get(HttpRouter.class);
                for (HttpRoute route : router) {
                    routes.add(route);
                }

                routes.addAll(module.getContext().getAll(HttpRoute.class));
            }
            return routes.iterator();
        });

        httpServer.awaitStop();
        httpServer.close();

        verveRoot.close();
    }
}
