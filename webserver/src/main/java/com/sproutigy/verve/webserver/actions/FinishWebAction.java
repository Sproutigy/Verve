package com.sproutigy.verve.webserver.actions;

import lombok.Data;

@Data
public class FinishWebAction implements WebAction {
    public final static FinishWebAction INSTANCE = new FinishWebAction();
}
