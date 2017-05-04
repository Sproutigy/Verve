package com.sproutigy.verve.webserver.actions;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RedirectWebAction implements WebAction {
    private String uri;
}
