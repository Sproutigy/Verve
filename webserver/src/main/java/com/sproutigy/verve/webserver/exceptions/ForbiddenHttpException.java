package com.sproutigy.verve.webserver.exceptions;

import io.netty.handler.codec.http.HttpResponseStatus;

public class ForbiddenHttpException extends HttpException {
    public ForbiddenHttpException() {
        super(HttpResponseStatus.FORBIDDEN);
    }
}
