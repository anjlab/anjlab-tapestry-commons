package com.anjlab.tapestry5.services.events;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.apache.tapestry5.ComponentEventCallback;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.plastic.InstanceContext;

public class PublisherImpl implements Publisher
{
    private Map<String, Map<String, ComponentResources>> hub = new HashMap<String, Map<String, ComponentResources>>();
    
    @Override
    public void subscribe(String eventType, Object listener)
    {
        Map<String, ComponentResources> subscribers = hub.get(eventType);
        
        if (subscribers == null)
        {
            subscribers = new HashMap<String, ComponentResources>();
            
            hub.put(eventType, subscribers);
        }
        
        try
        {
            Field instanceContextField = listener.getClass().getDeclaredField("instanceContext");
            
            instanceContextField.setAccessible(true);
            
            InstanceContext instanceContext = (InstanceContext) instanceContextField.get(listener);
            
            instanceContextField.setAccessible(false);
            
            ComponentResources resources = instanceContext.get(ComponentResources.class);
            
            if (resources == null)
            {
                return;
            }
            
            String listenerKey = resources.getCompleteId();
            
            if (subscribers.containsKey(listenerKey))
            {
                return;
            }
            
            subscribers.put(listenerKey, resources);
        }
        catch (Exception e)
        {
            return;
        }
    }

    @Override
    public boolean triggerEvent(String eventType, Object[] contextValues,
            ComponentEventCallback<?> callback)
    {
        Map<String, ComponentResources> subscribers = hub.get(eventType);
        
        if (subscribers == null)
        {
            return false;
        }
        
        boolean result = false;
        
        for (ComponentResources resources : subscribers.values())
        {
            result |= resources.triggerEvent(eventType, contextValues, callback);
        }
        
        return result;
    }
}
