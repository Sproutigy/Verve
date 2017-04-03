package com.sproutigy.verve.resources.exceptions;

public class ChildrenNotAvailableResourceException extends ResourceException {
    public ChildrenNotAvailableResourceException() {
        super("Cannot retrieve children of resource");
    }
}
