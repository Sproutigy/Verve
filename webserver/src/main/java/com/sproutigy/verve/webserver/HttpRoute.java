package com.sproutigy.verve.webserver;

import com.sproutigy.verve.webserver.annotations.HttpRoutes;
import lombok.*;

import java.util.*;

@RequiredArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class HttpRoute {
    @Getter
    private int order = 0;

    @Getter
    @NonNull
    private HttpRouteFilter filter;

    @Getter
    @NonNull
    private HttpHandler handler;

    @Getter
    private Class inputType;

    @Getter
    private Class outputType;

    public HttpRoute(int order, HttpRouteFilter filter, HttpHandler handler) {
        this(order, filter, handler, null, null);
    }


    @SuppressWarnings("unchecked")
    public static List<HttpRoute> from(Object instance) {
        Collection<com.sproutigy.verve.webserver.annotations.HttpRoute> annotations = new LinkedList<>();

        HttpRoutes annotationContainer = instance.getClass().getAnnotation(HttpRoutes.class);
        if (annotationContainer != null) {
            for (com.sproutigy.verve.webserver.annotations.HttpRoute annotation : annotationContainer.value()) {
                annotations.add(annotation);
            }
        } else {
            com.sproutigy.verve.webserver.annotations.HttpRoute annotation = instance.getClass().getAnnotation(com.sproutigy.verve.webserver.annotations.HttpRoute.class);
            if (annotation != null) {
                annotations.add(annotation);
            }
        }

        if (annotations.size() > 0) {
            List<HttpRoute> routes = new LinkedList<>();

            for (com.sproutigy.verve.webserver.annotations.HttpRoute annotation : annotations) {
                HttpHandler handler;
                if (instance instanceof HttpObjectHandler) {
                    handler = HttpObjectHandler.toBaseHandler(annotation.inputType(), (HttpObjectHandler) instance);
                } else {
                    handler = (HttpHandler) instance;
                }

                HttpRoute route = from(annotation, handler);
                routes.add(route);
            }

            return routes;
        } else {
            HttpHandler handler;
            if (instance instanceof HttpObjectHandler) {
                handler = HttpObjectHandler.toBaseHandler(Object.class, (HttpObjectHandler) instance);
            } else {
                handler = (HttpHandler) instance;
            }

            HttpRoute route = new HttpRoute(HttpRouteFilter.ALWAYS, handler);
            return Collections.singletonList(route);
        }
    }

    public static HttpRoute from(com.sproutigy.verve.webserver.annotations.HttpRoute annotation, HttpHandler handler) {
        HttpRouteFilter filter = new DefaultHttpRouteFilter(Arrays.asList(annotation.method()), Arrays.asList(annotation.path()));
        return new HttpRoute(annotation.order(), filter, handler, annotation.inputType(), annotation.outputType());
    }
}
