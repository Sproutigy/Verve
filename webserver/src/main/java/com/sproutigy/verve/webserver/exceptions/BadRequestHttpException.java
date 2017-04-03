package com.sproutigy.verve.webserver.exceptions;

import io.netty.handler.codec.http.HttpResponseStatus;

public class BadRequestHttpException extends HttpException {
    public BadRequestHttpException() {
        super(HttpResponseStatus.BAD_REQUEST);
    }
}
