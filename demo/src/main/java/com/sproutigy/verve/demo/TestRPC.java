package com.sproutigy.verve.demo;

import com.sproutigy.verve.webserver.HttpRequestContext;
import lombok.val;

//http://localhost:8083/rpc/test?jsonrpc=2.0&method=hello&id=1
public class TestRPC {
    public String hello() {
        val ctx = HttpRequestContext.get();
        System.out.println(ctx);
        return "world";
    }

    public long time() {
        return System.currentTimeMillis();
    }

    public String say(String text) {
        return "okay, " + text + "!";
    }
}
