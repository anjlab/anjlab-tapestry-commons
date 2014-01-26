package com.anjlab.tapestry5.services.events.internal;

import java.util.Collection;

import org.apache.tapestry5.plastic.PlasticMethod;


public interface PublisherConfiguration
{
    void addListener(String eventType, Object listener);
    void removeListener(String eventType, Object listener);
    
    void addTrigger(String eventType, PlasticMethod trigger);
    
    void addEventHandler(String value, PlasticMethod handler);
    
    void clear();
    
    Iterable<String> eventTypes();
    
    Iterable<?> listeners(String eventType);
    Iterable<?> triggers(String eventType);
    Iterable<?> handlers(String eventType);
    
    boolean isManagedEvent(String eventType);
    
    void addManagedEvents(Collection<String> managedEvents);
}
