package com.anjlab.tapestry5.services.events;

import org.apache.tapestry5.ComponentEventCallback;

public interface Publisher
{
    boolean triggerEvent(String eventType, Object[] contextValues, ComponentEventCallback<?> callback);

    void subscribe(String eventType, Object listener);
}
