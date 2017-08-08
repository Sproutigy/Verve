package com.sproutigy.verve.webserver.actions;

import com.sproutigy.verve.webserver.HttpRequestContext;
import lombok.Data;

@Data
public class FinishWebAction implements WebAction {
    public final static FinishWebAction INSTANCE = new FinishWebAction();

    @Override
    public Object execute(HttpRequestContext context) {
        return null;
    }
}
