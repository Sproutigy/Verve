package com.sproutigy.verve.webserver;

import com.sproutigy.commons.async.Promise;
import com.sproutigy.commons.binary.Binary;
import com.sproutigy.verve.webserver.exceptions.BadRequestHttpException;
import io.netty.handler.codec.http.cookie.Cookie;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface HttpRequest extends AutoCloseable {
    String getPath();


    @Nullable
    String getHeader(CharSequence name);

    String getHeaderRequired(CharSequence name) throws BadRequestHttpException;


    String getMethod();

    String getContextualPath();

    Promise<Binary> fetchData();

    Promise<Binary> fetchData(long limit);


    String getQueryString();

    @Nullable
    String getQueryParam(String name);

    Collection<String> getQueryParamNames();

    Map<String, String> getQueryParams();

    String getQueryParamRequired(String name) throws BadRequestHttpException;


    Set<Cookie> getCookies();

    @Nullable
    Cookie getCookie(CharSequence name);

    @Nullable
    String getCookieValue(CharSequence name);

    String getRemote();
}
