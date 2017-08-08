package com.sproutigy.verve.webserver.actions;

import com.sproutigy.verve.webserver.HttpRequestContext;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RedirectWebAction implements WebAction {
    private String uri;

    @Override
    public Object execute(HttpRequestContext context) {
        context.getResponse().redirect(getUri());
        return null;
    }
}
