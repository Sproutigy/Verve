package com.sproutigy.verve.webserver.exceptions;

import io.netty.handler.codec.http.HttpResponseStatus;

public class RequestEntityTooLargeHttpException extends HttpException {
    public RequestEntityTooLargeHttpException() {
        super(HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE);
    }
}
