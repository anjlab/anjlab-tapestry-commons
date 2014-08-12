/**
 * Copyright 2014 AnjLab
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
package com.anjlab.tapestry5.services.events.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.tapestry5.plastic.PlasticMethod;

public class PublisherConfigurationImpl implements PublisherConfiguration
{
    private static class Method
    {
        private final String className;
        private final String methodName;
        
        private Method(String className, String methodName)
        {
            this.className = className;
            this.methodName = methodName;
        }
        
        @Override
        public String toString()
        {
            return className + "." + methodName;
        }
    }
    
    private final Set<String> eventTypes = new TreeSet<String>();
    private final Map<String, List<Object>> listeners = new TreeMap<String, List<Object>>();
    private final Map<String, List<Method>> triggers = new TreeMap<String, List<Method>>();
    private final Map<String, List<Method>> handlers = new TreeMap<String, List<Method>>();
    
    private final Set<String> managedEventTypes = new HashSet<String>();
    
    @Override
    public void addManagedEvents(Collection<String> managedEvents)
    {
        for (String eventType : managedEvents)
        {
            this.managedEventTypes.add(eventType.toLowerCase());
        }
    }
    
    @Override
    public void addListener(String eventType, Object listener)
    {
        eventType = eventType.toLowerCase();
        List<Object> list = listeners.get(eventType);
        if (list == null)
        {
            list = new ArrayList<Object>();
            listeners.put(eventType, list);
        }
        list.add(listener);
        addEventType(eventType);
    }

    @Override
    public void removeListener(String eventType, Object listener)
    {
        eventType = eventType.toLowerCase();
        List<Object> list = listeners.get(eventType);
        if (list != null)
        {
            list.remove(listener);
        }
    }

    @Override
    public void addTrigger(String eventType, PlasticMethod trigger)
    {
        eventType = eventType.toLowerCase();
        List<Method> list = triggers.get(eventType);
        if (list == null)
        {
            list = new ArrayList<Method>();
            triggers.put(eventType, list);
        }
        list.add(createMethod(trigger));
        addEventType(eventType);
    }

    private Method createMethod(PlasticMethod fromPlasticMethod)
    {
        return new Method(fromPlasticMethod.getPlasticClass().getClassName(), fromPlasticMethod.getDescription().methodName);
    }

    @Override
    public void addEventHandler(String eventType, PlasticMethod handler)
    {
        eventType = eventType.toLowerCase();
        List<Method> list = handlers.get(eventType);
        if (list == null)
        {
            list = new ArrayList<Method>();
            handlers.put(eventType, list);
        }
        list.add(createMethod(handler));
        addEventType(eventType);
    }

    private void addEventType(String eventType)
    {
        eventTypes.add(eventType);
    }

    @Override
    public void clear()
    {
        listeners.clear();
        triggers.clear();
        handlers.clear();
        eventTypes.clear();
    }

    @Override
    public Iterable<String> eventTypes()
    {
        Set<String> eventTypes = new TreeSet<String>();
        eventTypes.addAll(listeners.keySet());
        eventTypes.add("*");
        return eventTypes;
    }

    @Override
    public Iterable<?> listeners(String eventType)
    {
        return listeners.get(eventType.toLowerCase());
    }

    @Override
    public boolean isManagedEvent(String eventType)
    {
        List<Object> list = listeners.get(eventType.toLowerCase());
        return managedEventTypes.contains(eventType.toLowerCase())
            || (list != null && !list.isEmpty());
    }
    
    @Override
    public Iterable<?> triggers(String eventType)
    {
        return triggers.get(eventType.toLowerCase());
    }

    @Override
    public Iterable<?> handlers(String eventType)
    {
        return handlers.get(eventType.toLowerCase());
    }

}
