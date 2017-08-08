package com.sproutigy.verve.webserver.handlers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.sproutigy.commons.async.Deferred;
import com.sproutigy.commons.async.FutureWatch;
import com.sproutigy.commons.async.Promise;
import com.sproutigy.commons.jsonright.jackson.JSON;
import com.sproutigy.verve.webserver.HttpHandler;
import com.sproutigy.verve.webserver.HttpRequest;
import com.sproutigy.verve.webserver.HttpRequestContext;
import com.sproutigy.verve.webserver.HttpUtil;
import com.sproutigy.verve.webserver.exceptions.BadRequestHttpException;
import com.sproutigy.verve.webserver.exceptions.MethodNotAllowedHttpException;
import com.sproutigy.verve.webserver.exceptions.UnsupportedMediaType;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
public class JSONRPCHttpHandler implements HttpHandler {

    public static final String JSON_RPC_SUPPORTED_VERSION = "2.0";

    public static final String FIELD_JSONRPC = "jsonrpc";
    public static final String FIELD_METHOD = "method";
    public static final String FIELD_PARAMS = "params";
    public static final String FIELD_ID = "id";

    @Data
    public class RPCReturn {
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String jsonrpc;

        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Object result;

        @JsonInclude(JsonInclude.Include.NON_NULL)
        private RPCError error;

        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String id;
    }

    @Data
    public class RPCError {
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Integer code;

        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String message;

        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Object data;
    }

    @Getter
    private Object instance;

    private ConcurrentHashMap<String, Method> methods = new ConcurrentHashMap<>();

    private ExecutorService executorService = Executors.newCachedThreadPool();

    private static Collection<String> UNCALLABLE_METHODS = new HashSet<>();
    static {
        for(Method method : Object.class.getMethods()) {
            UNCALLABLE_METHODS.add(method.getName());
        }
    }

    @Getter
    private int maxConcurrentCalls = 1; //TODO

    private boolean allowGET = true;

    public JSONRPCHttpHandler(Object instance) {
        this.instance = instance;
    }

    public JSONRPCHttpHandler(Object instance, boolean allowGET, int maxConcurrentCalls) {
        this.instance = instance;
        this.allowGET = allowGET;
        this.maxConcurrentCalls = maxConcurrentCalls;
    }


    @Override
    public Object handle(HttpRequestContext ctx) throws Exception {
        HttpUtil.cacheAvoid(ctx.getResponse());
        HttpRequest req = ctx.getRequest();

        String httpMethod = req.getMethod();
        if (Objects.equals(httpMethod, HttpMethod.OPTIONS.toString())) {
            ctx.getResponse().setHeaderIfNotSet("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        }
        if (Objects.equals(httpMethod, HttpMethod.GET.toString())) {
            String methodName;
            JsonNode jsonParams;
            String id = null;
            String jsonrpc = null;
            if (req.getQueryParams().containsKey(FIELD_JSONRPC)) {
                jsonrpc = req.getQueryParam(FIELD_JSONRPC);
                if (!Objects.equals(jsonrpc, JSON_RPC_SUPPORTED_VERSION)) {
                    throw new BadRequestHttpException();
                }

                methodName = req.getQueryParamRequired(FIELD_METHOD);
                String params = req.getQueryParam(FIELD_PARAMS);
                id = req.getQueryParam(FIELD_ID);
                jsonParams = params != null ? JSON.fromString(params).node() : null;
            } else {
                String p = req.getPath();
                methodName = p.substring(p.lastIndexOf('/') + 1);

                String qs = req.getQueryString();
                if (qs != null && !qs.isEmpty()) {
                    boolean withNames = qs.indexOf('=') >= 0;

                    if (withNames) {
                        jsonParams = JSON.newObjectNode();
                    } else {
                        jsonParams = JSON.newArrayNode();
                    }

                    for (Map.Entry<String, String> param : req.getQueryParams().entrySet()) {
                        String val;
                        if (withNames) {
                            val = param.getValue();
                        } else {
                            val = param.getKey();
                        }

                        JsonNode jsonNode;

                        if (val.startsWith("{") || val.startsWith("[") || val.startsWith("\"") || val.equals("null")) {
                            jsonNode = JSON.fromString(val).node();
                        } else {
                            jsonNode = new TextNode(val);
                        }

                        if (withNames) {
                            ((ObjectNode) jsonParams).set(param.getKey(), jsonNode);
                        } else {
                            ((ArrayNode) jsonParams).add(jsonNode);
                        }
                    }
                } else {
                    jsonParams = JSON.newArrayNode();
                }
            }

            Method method = getMethod(methodName, jsonParams);
            if (method == null) {
                throw new BadRequestHttpException();
            }

            if (Modifier.isPublic(method.getModifiers()) && !isAllowedGET(method)) {
                throw new MethodNotAllowedHttpException();
            }

            return executeRPC(ctx, jsonrpc, method, jsonParams, id);

        } else if (Objects.equals(httpMethod, HttpMethod.POST.toString())) {
            if (!req.getHeaderRequired(HttpHeaderNames.CONTENT_TYPE).startsWith("application/json")) {
                throw new UnsupportedMediaType();
            }

            Deferred deferred = Promise.defer();

            FutureWatch.listen(req.fetchData(), (result, data, cause) -> {
                if (result.isSuccess()) {
                    String methodName;
                    JsonNode jsonParams;
                    String id = null;
                    JSON json = JSON.fromBytes(data.asByteArray());
                    String jsonrpc = null;
                    if (json.nodeObject().has(FIELD_JSONRPC)) {
                        jsonrpc = json.nodeObject().get(FIELD_JSONRPC).asText();
                        methodName = json.nodeObject().get(FIELD_METHOD).asText();
                        jsonParams = json.nodeObject().get(FIELD_PARAMS);
                        id = json.nodeObject().get(FIELD_ID).asText();
                    } else {
                        methodName = HttpUtil.urldecode(req.getQueryString());
                        jsonParams = json.nodeObject();
                        if ((methodName == null || methodName.isEmpty()) && jsonParams.has("_")) {
                            methodName = jsonParams.get("_").asText();
                            ((ObjectNode)jsonParams).remove("_");
                        }
                    }

                    Method method = getMethod(methodName, jsonParams);
                    if (method == null) {
                        deferred.reject(new BadRequestHttpException());
                        return;
                    }

                    deferred.bindTo(executeRPC(ctx, jsonrpc, method, jsonParams, id));
                } else {
                    deferred.reject(cause);
                }
            });

            return deferred;
        } else {
            return null;
        }
    }

    protected Method getMethod(String methodName, JsonNode params) {
        //TODO: check param names

        int paramsCount = params == null ? 0 : params.size();
        return methods.computeIfAbsent(methodName, s -> {
            Method[] methods = instance.getClass().getMethods();
            for (int i = 0; i < methods.length; i++) {
                Method someMethod = methods[i];
                if (someMethod.getName().equals(s) && !UNCALLABLE_METHODS.contains(s)) {
                    if (someMethod.getParameterCount() == paramsCount) {
                        if (Modifier.isPublic(someMethod.getModifiers())) {
                            someMethod.setAccessible(true);
                            return someMethod;
                        }
                    }
                }
            }
            return null;
        });
    }

    protected boolean isAllowedGET(Method method) {
        return allowGET;
    }

    protected void beforeCall(HttpRequestContext ctx, Method method, String id) throws Exception {
    }

    protected RPCReturn afterCall(HttpRequestContext ctx, String jsonrpc, Method method, String id, boolean success, Object result, Throwable cause) {
        RPCReturn ret = new RPCReturn();
        ret.setJsonrpc(jsonrpc);
        if (success) {
            ret.setResult(result);
        } else {
            ret.setError(describeError(cause));
        }
        ret.setId(id);
        return ret;
    }

    protected RPCError describeError(Throwable cause) {
        RPCError rpcError = new RPCError();
        Object data = null;
        try {
            data = JSON.serialize(cause);
        } catch (Exception ignore) { }
        rpcError.setCode(getErrorCode(cause));
        rpcError.setData(data);
        rpcError.setMessage(cause.getMessage());
        return rpcError;
    }

    protected int getErrorCode(Throwable cause) {
        return 0;
    }

    protected Future executeRPC(HttpRequestContext ctx, String jsonrpc, Method method, JsonNode jsonParams, String id) {
        return executorService.submit(() -> {
            HttpRequestContext.set(ctx);

            RPCReturn result;
            Object[] params = null;
            try {
                beforeCall(ctx, method, id);

                int count = method.getParameterCount();
                params = new Object[count];
                Class[] types = method.getParameterTypes();

                if (count > 0 && jsonParams != null) {
                    Iterator<JsonNode> elementsIterator = jsonParams.elements();
                    for (int i = 0; i < count; i++) {
                        Class clazz = types[i];
                        JsonNode jsonParam;

                        JsonNode element = null;
                        if (elementsIterator.hasNext()) {
                            element = elementsIterator.next();
                        }

                        if (jsonParams.isArray()) {
                            jsonParam = jsonParams.get(i);
                        } else {
                            String paramName = method.getParameters()[i].getName();
                            jsonParam = jsonParams.get(paramName);
                            if (jsonParam == null) {
                                jsonParam = element;
                            }
                        }

                        params[i] = JSON.fromNode(jsonParam).deserialize(clazz);
                    }
                }

                try {
                    Object ret = method.invoke(instance, params);
                    result = afterCall(ctx, jsonrpc, method, id, true, ret, null);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            } catch(Throwable e) {
                log.error("RPC method invocation {}.{} with parameters ({}) error", method.getDeclaringClass().getName(), method.getName(), params, e);
                result = afterCall(ctx, jsonrpc, method, id, false, null, e);
            } finally {
                HttpRequestContext.remove();
            }

            if (result.getResult() == null && result.getError() == null) {
                ctx.getResponse().status(HttpResponseStatus.NO_CONTENT).end();
                return null;
            } else {
                return result;
            }
        });
    }

}
