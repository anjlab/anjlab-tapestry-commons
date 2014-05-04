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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.tapestry5.ComponentEventCallback;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.EventContext;
import org.apache.tapestry5.func.Predicate;
import org.apache.tapestry5.internal.services.RequestPageCache;
import org.apache.tapestry5.plastic.InstanceContext;
import org.apache.tapestry5.runtime.Component;
import org.apache.tapestry5.services.ComponentClasses;
import org.apache.tapestry5.services.InvalidationEventHub;
import org.apache.tapestry5.services.InvalidationListener;
import org.apache.tapestry5.services.Request;

import com.anjlab.tapestry5.pages.PublisherSupport;
import com.anjlab.tapestry5.services.events.internal.PublisherConfiguration;

public class PublisherImpl implements Publisher, InvalidationListener
{
    private RequestPageCache requestPageCache;
    private Request request;
    private PublisherConfiguration publisherConfiguration;
    
    private Map<String, Map<String, ComponentResources>> hub = new HashMap<String, Map<String, ComponentResources>>();
    
    public PublisherImpl(@ComponentClasses InvalidationEventHub invalidationHub,
                         PublisherConfiguration publisherConfiguration,
                         RequestPageCache requestPageCache,
                         Request request,
                         Collection<String> managedEvents)
    {
        invalidationHub.addInvalidationListener(this);
        this.publisherConfiguration = publisherConfiguration;
        this.publisherConfiguration.addManagedEvents(managedEvents);
        this.requestPageCache = requestPageCache;
        this.request = request;
    }
    
    @Override
    public boolean isActivePage(Component page)
    {
        String pageName = (String) request.getAttribute(PublisherSupport.PARAMETER_ACTIVE_PAGE);
        
        if (pageName == null)
        {
            pageName = request.getParameter(PublisherSupport.PARAMETER_ACTIVE_PAGE);
        }
        
        return page.getComponentResources().getPageName().equals(pageName);
    }
    
    @Override
    public void objectWasInvalidated()
    {
        hub.clear();
        publisherConfiguration.clear();
    }
    
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
            
            //  Overwrite if exists
            ComponentResources overwritten = subscribers.put(listenerKey, resources);
            
            if (overwritten != null)
            {
                publisherConfiguration.removeListener(eventType, overwritten);
            }
            
            publisherConfiguration.addListener(eventType, resources);
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
        
        List<ComponentResources> affectedResources = new ArrayList<ComponentResources>();
        
        for (ComponentResources resources : subscribers.values())
        {
            if (!isActivePage(resources.getPage()))
            {
                continue;
            }
            
            //  Force attaching target page to current request
            requestPageCache.get(resources.getPageName());
            
            affectedResources.add(resources);
        }
        
        boolean result = false;
        
        for (ComponentResources resources : affectedResources)
        {
            result |= function.accept(resources);
        }
        
        return result;
    }
}
