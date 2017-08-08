package com.sproutigy.verve.webserver.actions;

import com.sproutigy.verve.webserver.HttpRequestContext;

public interface WebAction {
    Object execute(HttpRequestContext context);
}
