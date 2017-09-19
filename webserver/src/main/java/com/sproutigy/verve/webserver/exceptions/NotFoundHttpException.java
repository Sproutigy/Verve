package com.sproutigy.verve.webserver.exceptions;

import io.netty.handler.codec.http.HttpResponseStatus;

public class NotFoundHttpException extends HttpException {
    public NotFoundHttpException() {
        super(HttpResponseStatus.NOT_FOUND);
    }
}
