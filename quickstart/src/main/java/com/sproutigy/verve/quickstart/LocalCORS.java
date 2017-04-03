package com.sproutigy.verve.quickstart;

import com.sproutigy.verve.webserver.HttpRequest;
import com.sproutigy.verve.webserver.HttpRequestContext;
import com.sproutigy.verve.webserver.HttpHandler;
import com.sproutigy.verve.webserver.HttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;

public class LocalCORS implements HttpHandler {
    @Override
    public Object handle(HttpRequestContext ctx) throws Exception {
        HttpRequest req = ctx.getRequest();
        HttpResponse resp = ctx.getResponse();

        String host = req.getHeader(HttpHeaderNames.HOST);
        if (host != null && host.startsWith("localhost")) {
            String headers = req.getHeader(HttpHeaderNames.ACCESS_CONTROL_REQUEST_HEADERS);
            if (headers != null) {
                resp.setHeader(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, headers);
            } else {
                headers = "Origin, X-Requested-With, Content-Type, Accept, Authorization";
            }
            resp.setHeader(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, headers);

            resp.setHeader("Access-Control-Allow-Methods", "HEAD, GET, POST, PUT, DELETE, OPTIONS");
            resp.setHeader("Access-Control-Allow-Origin", "*");
            resp.setHeader("Access-Control-Allow-Credentials", "true");

            if (req.getMethod().equals("OPTIONS")) {
                resp.end();
            }
        }

        return null;
    }
}
