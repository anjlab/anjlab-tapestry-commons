/**
 * Copyright 2014 AnjLab
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
