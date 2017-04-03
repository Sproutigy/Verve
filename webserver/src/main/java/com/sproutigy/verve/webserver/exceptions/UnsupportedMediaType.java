package com.sproutigy.verve.webserver.exceptions;

import io.netty.handler.codec.http.HttpResponseStatus;

public class UnsupportedMediaType extends HttpException {
    public UnsupportedMediaType() {
        super(HttpResponseStatus.UNSUPPORTED_MEDIA_TYPE);
    }
}
