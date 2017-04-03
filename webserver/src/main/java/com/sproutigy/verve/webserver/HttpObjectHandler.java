package com.sproutigy.verve.webserver;

import com.sproutigy.commons.async.Deferred;
import com.sproutigy.commons.async.FutureWatch;
import com.sproutigy.commons.async.Promise;

public interface HttpObjectHandler<IN, OUT> {
    OUT handle(IN input, HttpRequestContext ctx) throws Exception;

    @SuppressWarnings("unchecked")
    static <IN, OUT> HttpHandler toBaseHandler(Class<IN> inputClass, HttpObjectHandler<IN, OUT> httpObjectHandler) {
        return ctx -> {
            Deferred future = Promise.defer();
            FutureWatch.listen(ctx.fetchDataAs(inputClass), (result, value, cause) -> {
                if (result.isSuccess()) {
                    try {
                        OUT out = httpObjectHandler.handle(value, ctx);
                        future.resolve(out);
                    } catch (Throwable e) {
                        future.reject(e);
                    }
                } else {
                    future.reject(cause);
                }
            });
            return future;
        };
    }
}
