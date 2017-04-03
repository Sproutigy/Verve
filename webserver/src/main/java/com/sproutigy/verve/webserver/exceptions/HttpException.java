package com.sproutigy.verve.webserver.exceptions;

import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@EqualsAndHashCode
@ToString
public class HttpException extends Exception {
    @Getter
    private int statusCode;
    @Getter
    private String statusText;

    public HttpException(HttpResponseStatus status) {
        this(status.code(), status.reasonPhrase(), null);
    }

    public HttpException(HttpResponseStatus status, Throwable cause) {
        this(status.code(), status.reasonPhrase(), cause);
    }

    public HttpException(int statusCode, String statusText) {
        this(statusCode, statusText, null);
    }

    public HttpException(int statusCode, String statusText, Throwable cause) {
        super(statusCode + " " + statusText, cause);
        this.statusCode = statusCode;
        this.statusText = statusText;
    }
}
