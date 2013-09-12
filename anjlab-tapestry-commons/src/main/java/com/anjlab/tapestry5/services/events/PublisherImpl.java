/**
 * Copyright 2013 AnjLab
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.anjlab.tapestry5.services.events;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.apache.tapestry5.ComponentEventCallback;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.EventContext;
import org.apache.tapestry5.func.Predicate;
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
    public boolean triggerContextEvent(final String eventType,
                                final EventContext context,
                                final ComponentEventCallback<?> callback)
    {
        return trigger(eventType, new Predicate<ComponentResources>()
        {
            @Override
            public boolean accept(ComponentResources resources)
            {
                return resources.triggerContextEvent(eventType, context, callback);
            }
        });
    }
    
    @Override
    public boolean triggerEvent(final String eventType,
                                final Object[] contextValues,
                                final ComponentEventCallback<?> callback)
    {
        return trigger(eventType, new Predicate<ComponentResources>()
        {
            @Override
            public boolean accept(ComponentResources resources)
            {
                return resources.triggerEvent(eventType, contextValues, callback);
            }
        });
    }
    
    protected boolean trigger(String eventType, Predicate<ComponentResources> function)
    {
        Map<String, ComponentResources> subscribers = hub.get(eventType);
        
        if (subscribers == null)
        {
            return false;
        }
        
        boolean result = false;
        
        for (ComponentResources resources : subscribers.values())
        {
            result |= function.accept(resources);
        }
        
        return result;
    }
}
