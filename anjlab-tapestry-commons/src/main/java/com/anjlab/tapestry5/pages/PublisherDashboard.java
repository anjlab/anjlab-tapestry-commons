package com.anjlab.tapestry5.pages;

import java.util.List;

import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.annotations.WhitelistAccessOnly;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.ComponentClassResolver;
import org.apache.tapestry5.services.ComponentSource;

import com.anjlab.tapestry5.services.events.internal.PublisherConfiguration;

@WhitelistAccessOnly
public class PublisherDashboard
{
    @Inject private PublisherConfiguration publisherConfiguration;
    
    @Inject private ComponentClassResolver componentClassResolver;
    @Inject private ComponentSource componentSource;
    
    public Iterable<String> getEventTypes()
    {
        return publisherConfiguration.eventTypes();
    }
    
    @Property private String eventType;
    
    @Property(read=false) private Object listener;
    @Property private Object trigger;
    @Property private Object handler;
    
    public Object getListener()
    {
        if (listener instanceof ComponentResources)
        {
            ComponentResources resources = (ComponentResources) listener;
            return resources.getCompleteId();
        }
        return listener;
    }
    
    public void onForceLoad()
    {
        List<String> pageNames = componentClassResolver.getPageNames();
        for (String pageName : pageNames)
        {
            componentSource.getPage(pageName);
        }
    }
    
    public Iterable<?> getEventListeners()
    {
        return publisherConfiguration.listeners(eventType);
    }
    
    public Iterable<?> getEventTriggers()
    {
        return publisherConfiguration.triggers(eventType);
    }
    
    public Iterable<?> getEventHandlers()
    {
        return publisherConfiguration.handlers(eventType);
    }
}
