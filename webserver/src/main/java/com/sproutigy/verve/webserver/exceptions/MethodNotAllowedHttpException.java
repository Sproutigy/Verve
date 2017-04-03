package com.sproutigy.verve.webserver.exceptions;

import io.netty.handler.codec.http.HttpResponseStatus;

public class MethodNotAllowedHttpException extends HttpException {
    public MethodNotAllowedHttpException() {
        super(HttpResponseStatus.METHOD_NOT_ALLOWED);
    }
}
